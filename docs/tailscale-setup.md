# Tailscale setup (Bop-Search)

Private WAN between the home PC (generation host) and Android phone. PC is **not** always on — phone works offline and syncs when the PC (and Tailscale) come back.

## A. Account / tailnet (once)

1. Create or sign in at https://login.tailscale.com (Personal free is fine).
2. Admin console → **DNS**: confirm **MagicDNS** is on.
3. Note your MagicDNS suffix (e.g. `….ts.net`) from DNS or Machines.
4. Do **not** enable an exit node for the phone (battery + breaks split-tunnel intent).
5. Optional later: ACLs locking phone → only PC API ports — skip for day one.

Docs: https://tailscale.com/docs/features/magicdns · https://tailscale.com/pricing

## B. Home PC (generation host)

1. Install Tailscale for your OS (Windows / Linux / macOS).
2. Sign in with the same account; approve the machine if prompted.
3. Rename the machine to something stable (e.g. `powerspec`) — this becomes the MagicDNS short name.
4. Survive reboot / no interactive login:
   - **Linux:** `tailscaled` as a system service (`systemctl enable --now tailscaled`), then `tailscale up`.
   - **Windows:** Preferences → **Run unattended** (or `tailscale up --unattended=true`) so it comes up before login.
   - **macOS:** no true unattended-as-system yet; keep user session / login item aware.
5. Verify: `tailscale status` shows Self online; note Tailscale IP (`100.x`) and DNS name `powerspec.<suffix>`.
6. Bind the Bop-Search API / file server to the Tailscale IP or `0.0.0.0`, with firewall so only the tailnet can reach it (allow from `100.64.0.0/10` when you tighten later).
7. Start Bop-Search services **after** Tailscale is up (`tailscale wait` on Linux helps with ordering).

Docs: https://tailscale.com/docs/how-to/run-unattended · https://tailscale.com/docs/reference/tailscaled

## C. Android phone

1. Install Tailscale from Play (or F-Droid); use **v1.70+** for app split tunneling.
2. Sign in to the same tailnet; grant VPN permission; connect.
3. Enable **Always-on VPN** for Tailscale if you want reconnect after radio flips — but **do not** enable "Block connections without VPN" until split tunnel is validated.
4. **Split tunnel:**
   - Avatar → **App-based split tunneling** → **Exclude** mode. Leave **Bop-Search** unchecked so it uses Tailscale. Check-mark apps that must bypass the VPN.
   - True "only Bop-Search through Tailscale" may need MDM `IncludedPackageNames` (see Tailscale Android split-tunneling docs). Test carefully if you use Include mode.
5. Battery: Apps → Tailscale → unrestricted; don't use as exit node.
6. Smoke test while PC is on: hit `http://powerspec:PORT/` or `http://100.x.y.z:PORT/` from a tunneled context.

Docs: https://tailscale.com/docs/features/client/android-app-split-tunneling

## D. MagicDNS + peer presence

1. Prefer `powerspec` / FQDN via MagicDNS; keep the `100.x` IP as app fallback.
2. Presence ≠ DNS: MagicDNS can still resolve when the peer is **offline**. For "PC online?" use Tailscale status / a health ping to the API with timeout + retry.
3. Treat offline as normal: queue on phone, drain when health check succeeds after PC returns.
4. After Wi-Fi↔cellular flips, if the peer looks stuck, toggle Tailscale off/on once.

## E. Intermittent PC gotchas

1. Sleep/hibernate can leave Tailscale stale until wake — prefer a machine that fully boots, or disable sleep when "available for Bop-Search."
2. Windows without **Run unattended**: Tailscale may be down until someone logs in.
3. Services bound only to LAN ethernet won't be reachable on Tailscale — bind Tailscale IP or all interfaces.
4. Large downloads over DERP are slower; direct path appears when NAT allows.
5. Don't use Funnel/public URLs for the private API.
6. Phone local library + durable job queue remain required; Tailscale does not queue work while the PC is off.

## F. Done when

- [ ] Both devices show in Machines admin console
- [ ] MagicDNS name for PC works from a tunneled Android context (or IP fallback documented)
- [ ] Unattended/boot start verified after a cold power-on of the PC
- [ ] Android split-tunnel config documented
- [ ] Manual: PC off → phone plays local / queues; PC on → phone reaches API within ~1–2 min
