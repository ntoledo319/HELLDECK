package com.helldeck.billing

import android.app.Activity
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.android.billingclient.api.*
import com.helldeck.AppCtx
import com.helldeck.BuildConfig
import com.helldeck.settings.helldeckDataStore
import com.helldeck.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * PurchaseManager - Singleton handling Google Play Billing for HELLDECK
 *
 * Products:
 * - helldeck_premium ($4.99) - Unlocks all 9 premium games
 * - helldeck_tip ($1.99) - Optional tip jar / "buy me a coffee"
 *
 * Tester Mode:
 * - When BuildConfig.UNLOCK_ALL is true (tester build variant), all games are unlocked
 * - When BuildConfig.DEBUG is true, also unlocks all games as fallback for development
 */
object PurchaseManager : PurchasesUpdatedListener {

    // Product IDs
    const val PRODUCT_PREMIUM = "helldeck_premium"
    const val PRODUCT_TIP = "helldeck_tip"

    // DataStore keys
    private val KEY_PREMIUM_UNLOCKED = booleanPreferencesKey("premium_unlocked")
    private val KEY_TIP_PURCHASED = booleanPreferencesKey("tip_purchased")
    private val KEY_REDEEMED_CODE = stringPreferencesKey("redeemed_promo_code")

    // Free games that are always accessible
    val FREE_GAMES = setOf(
        "ROAST_CONSENSUS",
        "POISON_PITCH",
        "FILL_IN_FINISHER",
        "SCATTERBLAST",
        "CONFESSION_OR_CAP",
    )

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var billingClient: BillingClient? = null
    private var isInitialized = false

    private val _isPremiumUnlocked = MutableStateFlow(false)
    val isPremiumUnlocked: StateFlow<Boolean> = _isPremiumUnlocked.asStateFlow()

    private val _isTipPurchased = MutableStateFlow(false)
    val isTipPurchased: StateFlow<Boolean> = _isTipPurchased.asStateFlow()

    private val _billingConnectionState = MutableStateFlow(BillingConnectionState.DISCONNECTED)
    val billingConnectionState: StateFlow<BillingConnectionState> = _billingConnectionState.asStateFlow()

    private val _purchaseError = MutableStateFlow<String?>(null)
    val purchaseError: StateFlow<String?> = _purchaseError.asStateFlow()

    // Product details cache
    private var premiumProductDetails: ProductDetails? = null
    private var tipProductDetails: ProductDetails? = null

    enum class BillingConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR
    }

    /**
     * Check if all games should be unlocked (tester mode or debug build)
     */
    fun isUnlockAllMode(): Boolean {
        return try {
            BuildConfig.UNLOCK_ALL || BuildConfig.DEBUG
        } catch (e: Exception) {
            // UNLOCK_ALL field may not exist in older builds
            BuildConfig.DEBUG
        }
    }

    /**
     * Check if a specific game is unlocked
     */
    fun isGameUnlocked(gameId: String): Boolean {
        // Tester mode unlocks everything
        if (isUnlockAllMode()) return true

        // Free games are always unlocked
        if (gameId in FREE_GAMES) return true

        // Check premium status
        return _isPremiumUnlocked.value
    }

    /**
     * Initialize billing client and load persisted state
     */
    fun initBilling(context: Context) {
        if (isInitialized) {
            Logger.d("PurchaseManager already initialized")
            return
        }

        Logger.i("Initializing PurchaseManager")

        // Load persisted purchase state
        scope.launch {
            loadPersistedState()
        }

        // Skip billing setup in tester mode
        if (isUnlockAllMode()) {
            Logger.i("Tester/Debug mode - skipping billing setup, all games unlocked")
            _isPremiumUnlocked.value = true
            isInitialized = true
            return
        }

        // Initialize billing client
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        connectToBilling()
        isInitialized = true
    }

    private fun connectToBilling() {
        _billingConnectionState.value = BillingConnectionState.CONNECTING

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Logger.i("Billing client connected successfully")
                    _billingConnectionState.value = BillingConnectionState.CONNECTED

                    // Query product details and existing purchases
                    scope.launch {
                        queryProductDetails()
                        restorePurchases()
                    }
                } else {
                    Logger.e("Billing setup failed: ${billingResult.debugMessage}")
                    _billingConnectionState.value = BillingConnectionState.ERROR
                    _purchaseError.value = "Billing setup failed: ${billingResult.debugMessage}"
                }
            }

            override fun onBillingServiceDisconnected() {
                Logger.w("Billing service disconnected")
                _billingConnectionState.value = BillingConnectionState.DISCONNECTED

                // Retry connection
                scope.launch {
                    kotlinx.coroutines.delay(3000)
                    connectToBilling()
                }
            }
        })
    }

    private suspend fun queryProductDetails() {
        val client = billingClient ?: return

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_PREMIUM)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_TIP)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        withContext(Dispatchers.IO) {
            client.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    productDetailsList.forEach { details ->
                        when (details.productId) {
                            PRODUCT_PREMIUM -> premiumProductDetails = details
                            PRODUCT_TIP -> tipProductDetails = details
                        }
                    }
                    Logger.i("Product details loaded: ${productDetailsList.size} products")
                } else {
                    Logger.e("Failed to query product details: ${billingResult.debugMessage}")
                }
            }
        }
    }

    /**
     * Launch purchase flow for premium unlock
     */
    fun purchasePremium(activity: Activity) {
        val productDetails = premiumProductDetails
        if (productDetails == null) {
            _purchaseError.value = "Product not available. Please try again later."
            Logger.e("Premium product details not available")
            return
        }

        launchPurchaseFlow(activity, productDetails)
    }

    /**
     * Launch purchase flow for tip jar
     */
    fun purchaseTip(activity: Activity) {
        val productDetails = tipProductDetails
        if (productDetails == null) {
            _purchaseError.value = "Product not available. Please try again later."
            Logger.e("Tip product details not available")
            return
        }

        launchPurchaseFlow(activity, productDetails)
    }

    private fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
        val client = billingClient
        if (client == null || _billingConnectionState.value != BillingConnectionState.CONNECTED) {
            _purchaseError.value = "Billing not connected. Please try again."
            return
        }

        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
            ?: productDetails.oneTimePurchaseOfferDetails?.let { "" }
            ?: run {
                _purchaseError.value = "Invalid product configuration"
                return
            }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .apply {
                if (offerToken.isNotEmpty()) {
                    setOfferToken(offerToken)
                }
            }
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val result = client.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _purchaseError.value = "Failed to launch purchase: ${result.debugMessage}"
            Logger.e("Failed to launch billing flow: ${result.debugMessage}")
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    scope.launch {
                        handlePurchase(purchase)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Logger.i("User canceled purchase")
                _purchaseError.value = null
            }
            else -> {
                Logger.e("Purchase error: ${billingResult.debugMessage}")
                _purchaseError.value = "Purchase failed: ${billingResult.debugMessage}"
            }
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Acknowledge the purchase if not already acknowledged
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { result ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        Logger.i("Purchase acknowledged: ${purchase.products}")
                    } else {
                        Logger.e("Failed to acknowledge purchase: ${result.debugMessage}")
                    }
                }
            }

            // Update local state based on product
            purchase.products.forEach { productId ->
                when (productId) {
                    PRODUCT_PREMIUM -> {
                        _isPremiumUnlocked.value = true
                        persistPremiumState(true)
                        Logger.i("Premium unlocked!")
                    }
                    PRODUCT_TIP -> {
                        _isTipPurchased.value = true
                        persistTipState(true)
                        Logger.i("Tip purchased - thank you!")
                    }
                }
            }
        }
    }

    /**
     * Restore purchases (e.g., on reinstall or new device)
     */
    suspend fun restorePurchases(): Boolean {
        val client = billingClient ?: return false

        if (_billingConnectionState.value != BillingConnectionState.CONNECTED) {
            Logger.w("Cannot restore purchases - billing not connected")
            return false
        }

        return withContext(Dispatchers.IO) {
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()

            val result = client.queryPurchasesAsync(params)

            if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                var foundPurchases = false
                result.purchasesList.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        purchase.products.forEach { productId ->
                            when (productId) {
                                PRODUCT_PREMIUM -> {
                                    _isPremiumUnlocked.value = true
                                    persistPremiumState(true)
                                    foundPurchases = true
                                    Logger.i("Restored premium purchase")
                                }
                                PRODUCT_TIP -> {
                                    _isTipPurchased.value = true
                                    persistTipState(true)
                                    foundPurchases = true
                                    Logger.i("Restored tip purchase")
                                }
                            }
                        }
                    }
                }
                foundPurchases
            } else {
                Logger.e("Failed to restore purchases: ${result.billingResult.debugMessage}")
                false
            }
        }
    }

    /**
     * Unlock premium via promo code (called from PromoCodeManager)
     */
    fun unlockPremiumViaPromoCode(code: String) {
        scope.launch {
            _isPremiumUnlocked.value = true
            persistPremiumState(true)
            persistRedeemedCode(code)
            Logger.i("Premium unlocked via promo code: $code")
        }
    }

    private suspend fun loadPersistedState() {
        try {
            val prefs = AppCtx.ctx.helldeckDataStore.data.first()
            _isPremiumUnlocked.value = prefs[KEY_PREMIUM_UNLOCKED] ?: false
            _isTipPurchased.value = prefs[KEY_TIP_PURCHASED] ?: false
            Logger.d("Loaded purchase state: premium=${_isPremiumUnlocked.value}, tip=${_isTipPurchased.value}")
        } catch (e: Exception) {
            Logger.e("Failed to load purchase state", e)
        }
    }

    private suspend fun persistPremiumState(unlocked: Boolean) {
        try {
            AppCtx.ctx.helldeckDataStore.edit { prefs ->
                prefs[KEY_PREMIUM_UNLOCKED] = unlocked
            }
        } catch (e: Exception) {
            Logger.e("Failed to persist premium state", e)
        }
    }

    private suspend fun persistTipState(purchased: Boolean) {
        try {
            AppCtx.ctx.helldeckDataStore.edit { prefs ->
                prefs[KEY_TIP_PURCHASED] = purchased
            }
        } catch (e: Exception) {
            Logger.e("Failed to persist tip state", e)
        }
    }

    private suspend fun persistRedeemedCode(code: String) {
        try {
            AppCtx.ctx.helldeckDataStore.edit { prefs ->
                prefs[KEY_REDEEMED_CODE] = code
            }
        } catch (e: Exception) {
            Logger.e("Failed to persist redeemed code", e)
        }
    }

    /**
     * Get the redeemed promo code (if any)
     */
    fun getRedeemedCodeFlow(): Flow<String?> {
        return AppCtx.ctx.helldeckDataStore.data.map { prefs ->
            prefs[KEY_REDEEMED_CODE]
        }
    }

    /**
     * Get premium price string for display
     */
    fun getPremiumPriceString(): String {
        return premiumProductDetails?.oneTimePurchaseOfferDetails?.formattedPrice ?: "$4.99"
    }

    /**
     * Get tip price string for display
     */
    fun getTipPriceString(): String {
        return tipProductDetails?.oneTimePurchaseOfferDetails?.formattedPrice ?: "$1.99"
    }

    /**
     * Clear purchase error
     */
    fun clearError() {
        _purchaseError.value = null
    }

    /**
     * Clean up resources
     */
    fun destroy() {
        billingClient?.endConnection()
        billingClient = null
        isInitialized = false
    }
}
