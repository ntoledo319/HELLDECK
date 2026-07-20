// Join flow (spec 6.1 /:code): name (14 max) + 16-slot devil picker + one-tap 18+ attest.
import { useState } from 'preact/hooks';
import { cleanName, NAME_MAX } from '../logic';
import { Devil } from './bits';

export function JoinScreen({
  code,
  error,
  onJoin,
}: {
  code: string;
  error: string | null;
  onJoin: (name: string, avatar: number) => void;
}) {
  const [name, setName] = useState('');
  const [avatar, setAvatar] = useState(-1);
  const [attested, setAttested] = useState(false);
  const ok = cleanName(name).length > 0 && avatar >= 0 && attested;

  return (
    <main class="screen join">
      <header class="join-head">
        <span class="wordmark">
          HELL<em>DECK</em>
        </span>
        <span class="join-code">{code}</span>
      </header>
      {error && <div class="err-banner">{error}</div>}

      <label class="field-label" for="join-name">
        WHO'S DESCENDING?
      </label>
      <input
        id="join-name"
        class="name-input"
        maxLength={NAME_MAX}
        autocomplete="off"
        spellcheck={false}
        placeholder="YOUR NAME"
        value={name}
        onInput={(e) => setName((e.target as HTMLInputElement).value)}
      />

      <div class="field-label">PICK YOUR DEVIL</div>
      <div class="avatar-grid">
        {Array.from({ length: 16 }, (_, i) => (
          <button key={i} class={avatar === i ? 'avatar sel' : 'avatar'} onClick={() => setAvatar(i)}>
            <Devil n={i} size={44} />
          </button>
        ))}
      </div>

      <button class={attested ? 'attest done' : 'attest'} onClick={() => setAttested(!attested)}>
        {attested ? '18+ — ATTESTED' : "I'M 18+ AND I CAN TAKE A JOKE"}
      </button>

      <button class="btn-blood big" disabled={!ok} onClick={() => onJoin(cleanName(name), avatar)}>
        ENTER HELL
      </button>
    </main>
  );
}
