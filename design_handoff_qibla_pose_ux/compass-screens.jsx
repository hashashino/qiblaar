// Compass screens — all 7 states.
// Each is a self-contained <Phone> with the same layout shell so users
// can compare pose / accuracy / lock states side-by-side.

// ─────────────────────────────────────────────────────────────
// Shared layout shell for compass screens
// ─────────────────────────────────────────────────────────────
function CompassShell({ children, locationLabel = 'Singapore · Tampines', accuracy = 'good' }) {
  return (
    <div className="phone-bg" style={{ background: 'var(--bg-app)', display:'flex', flexDirection:'column' }}>
      {/* Top header */}
      <div style={{ padding: '8px 20px 6px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ display: 'flex', flexDirection: 'column' }}>
          <span style={{ fontSize: 11, color: 'var(--text-4)', fontWeight: 600, letterSpacing: 0.6, textTransform: 'uppercase' }}>Qibla Compass</span>
          <span style={{ fontSize: 14, color: 'var(--text)', fontWeight: 700, marginTop: 2 }}>{locationLabel}</span>
        </div>
        <Chip tone={accuracy === 'good' ? 'green' : accuracy === 'warn' ? 'amber' : 'red'}>
          {accuracy === 'good' ? <><Dot color="#22c55e"/> ±5°</>
           : accuracy === 'warn' ? <><Dot color="#F59E0B"/> ±18°</>
           : <><Dot color="#EF4444"/> Inaccurate</>}
        </Chip>
      </div>

      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '8px 20px 0', overflow: 'hidden' }}>
        {children}
      </div>

      <BottomNav active="compass"/>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// 1. Finding location
// ─────────────────────────────────────────────────────────────
function CompassFindingLocation() {
  return (
    <CompassShell locationLabel="Locating…" accuracy="warn">
      <div style={{ marginTop: 24, opacity: 0.5 }}>
        <CompassDisk azimuth={0} qibla={0} size={260} dim accuracy="unreliable"
          centerLabel={
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8 }}>
              <div style={{
                width: 44, height: 44, borderRadius: 999,
                background: 'rgba(245,158,11,0.16)', border: '1px solid rgba(245,158,11,0.4)',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                color: '#F59E0B',
              }}>
                <Icon.GPS width={22} height={22}/>
              </div>
              <span style={{ fontSize: 11, color: 'var(--text-3)', fontWeight: 700, letterSpacing: 0.8, textTransform: 'uppercase' }}>Searching</span>
            </div>
          }
        />
      </div>
      <div style={{ marginTop: 32, textAlign: 'center', maxWidth: 320 }}>
        <div style={{ fontSize: 18, fontWeight: 700, color: 'var(--text)' }}>Finding your location</div>
        <div style={{ marginTop: 6, fontSize: 13, color: 'var(--text-3)', lineHeight: 1.5 }}>
          Step outside or near a window for a faster lock. Qibla bearing needs your exact position.
        </div>
      </div>
      <div style={{ flex: 1 }}/>
      <button className="btn btn-link" style={{ marginBottom: 16 }}>
        Use pre-set direction instead
      </button>
    </CompassShell>
  );
}

// ─────────────────────────────────────────────────────────────
// 2. Phone tilted — wrong pose
// ─────────────────────────────────────────────────────────────
function CompassPoseHint() {
  return (
    <CompassShell accuracy="warn">
      <div style={{
        flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center',
        justifyContent: 'center', textAlign: 'center', padding: '0 8px',
      }}>
        <PhoneFlatIllustration />
        <div style={{ marginTop: 28, fontSize: 22, fontWeight: 800, color: 'var(--text)', letterSpacing: -0.01 }}>
          Lay phone flat
        </div>
        <div style={{ marginTop: 8, fontSize: 14, color: 'var(--text-2)', lineHeight: 1.55, maxWidth: 290 }}>
          Place your phone face-up on a flat surface — like a table or floor — so the compass can read true heading.
        </div>

        {/* Inline tilt meter */}
        <div style={{
          marginTop: 22, padding: '10px 14px',
          background: 'rgba(255,255,255,0.04)', border: '1px solid var(--hairline)',
          borderRadius: 14, display: 'flex', alignItems: 'center', gap: 10,
        }}>
          <Icon.Warn width={16} height={16} style={{ color: '#F59E0B' }}/>
          <div style={{ fontSize: 12.5, fontWeight: 600, color: 'var(--text-2)' }}>
            Tilted <span style={{ color: '#F59E0B' }}>32°</span> — needs to be under 15°
          </div>
        </div>
      </div>
      <button className="btn btn-link" style={{ marginBottom: 16 }}>
        Why does this matter?
      </button>
    </CompassShell>
  );
}

// PhoneFlatIllustration lives in pose-illustrations.jsx

// ─────────────────────────────────────────────────────────────
// 3. Calibration figure-8
// ─────────────────────────────────────────────────────────────
function CompassCalibrating() {
  return (
    <CompassShell accuracy="bad">
      <div style={{
        flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center',
        justifyContent: 'center', textAlign: 'center', padding: '0 8px',
      }}>
        <Figure8Illustration />
        <div style={{ marginTop: 24, fontSize: 22, fontWeight: 800, color: 'var(--text)' }}>
          Calibrate compass
        </div>
        <div style={{ marginTop: 8, fontSize: 14, color: 'var(--text-2)', lineHeight: 1.55, maxWidth: 290 }}>
          Move your phone slowly in a <strong style={{ color: 'var(--text)' }}>figure-8</strong> pattern through the air. About 5 seconds.
        </div>
        {/* progress */}
        <div style={{
          marginTop: 22, width: 220, height: 6, borderRadius: 999,
          background: 'rgba(255,255,255,0.08)', overflow: 'hidden',
        }}>
          <div style={{ width: '62%', height: '100%', background: 'linear-gradient(90deg, #F59E0B, #22c55e)', borderRadius: 999 }}/>
        </div>
        <div style={{ marginTop: 8, fontSize: 12, color: 'var(--text-3)', fontWeight: 600 }}>62% · keep going</div>
      </div>
      <button className="btn btn-link" style={{ marginBottom: 16 }}>
        Skip for now
      </button>
    </CompassShell>
  );
}

function Figure8Illustration() {
  return (
    <svg width="220" height="140" viewBox="0 0 220 140">
      <defs>
        <linearGradient id="figEightGrad" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0%" stopColor="#22c55e" stopOpacity="0"/>
          <stop offset="50%" stopColor="#22c55e"/>
          <stop offset="100%" stopColor="#22c55e" stopOpacity="0"/>
        </linearGradient>
      </defs>
      {/* dashed guide path */}
      <path d="M55 70 C 55 30, 105 30, 110 70 C 115 110, 165 110, 165 70 C 165 30, 115 30, 110 70 C 105 110, 55 110, 55 70 Z"
        fill="none" stroke="rgba(255,255,255,0.16)" strokeWidth="2" strokeDasharray="4 5"/>
      {/* progress arc */}
      <path d="M55 70 C 55 30, 105 30, 110 70 C 115 110, 165 110, 165 70"
        fill="none" stroke="url(#figEightGrad)" strokeWidth="3.5" strokeLinecap="round"/>
      {/* phone token */}
      <g transform="translate(150 88) rotate(-18)">
        <rect x="-12" y="-20" width="24" height="40" rx="4" fill="#1A1C34" stroke="#fff" strokeWidth="1.5"/>
        <circle cx="0" cy="-15" r="1.5" fill="#fff"/>
        <circle cx="0" cy="14" r="2" fill="none" stroke="#fff"/>
      </g>
    </svg>
  );
}

// ─────────────────────────────────────────────────────────────
// 4. Ready — perfect state
// ─────────────────────────────────────────────────────────────
function CompassReady() {
  return (
    <CompassShell accuracy="good">
      <div style={{ marginTop: 14 }}>
        <CompassDisk azimuth={42} qibla={295} size={282} accuracy="high"/>
      </div>
      <div style={{ marginTop: 14 }}>
        <BearingReadout qibla={295} distance="6,420 km"/>
      </div>

      {/* Signal trio */}
      <SignalRow />

      <div style={{ flex: 1 }}/>
      <div style={{ display: 'flex', gap: 10, width: '100%', marginBottom: 12 }}>
        <button className="btn btn-ghost" style={{ flex: 1 }}>
          <Icon.Lock width={16} height={16}/> Pre-set direction
        </button>
        <button className="btn btn-quiet" style={{ width: 50, padding: 0 }} aria-label="Recalibrate">
          <Icon.RotateCcw width={18} height={18}/>
        </button>
      </div>
    </CompassShell>
  );
}

function SignalRow() {
  const Pill = ({ icon, label, value, ok = 'good' }) => {
    const color = ok === 'good' ? '#22c55e' : ok === 'warn' ? '#F59E0B' : '#EF4444';
    return (
      <div style={{
        flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4,
        padding: '10px 8px', background: 'rgba(255,255,255,0.03)',
        border: '1px solid var(--hairline)', borderRadius: 12,
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 5, color: 'var(--text-3)', fontSize: 10.5, fontWeight: 700, letterSpacing: 0.6, textTransform: 'uppercase' }}>
          {icon} {label}
        </div>
        <div style={{ fontSize: 13, fontWeight: 700, color: color }}>{value}</div>
      </div>
    );
  };
  return (
    <div style={{ marginTop: 16, display: 'flex', gap: 8, width: '100%' }}>
      <Pill icon={<Icon.GPS width={11} height={11}/>}     label="GPS"     value="±3m" />
      <Pill icon={<Icon.Compass width={11} height={11}/>} label="Compass" value="±5°" />
      <Pill icon={<Icon.Magnet width={11} height={11}/>}  label="Magnetic" value="48µT" />
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// 5. Mild magnetic interference
// ─────────────────────────────────────────────────────────────
function CompassInterferenceMild() {
  return (
    <CompassShell accuracy="warn">
      {/* Inline warning bar */}
      <div style={{
        width: '100%', marginBottom: 4, padding: '10px 12px',
        background: 'rgba(245,158,11,0.10)', border: '1px solid rgba(245,158,11,0.30)',
        borderRadius: 12, display: 'flex', alignItems: 'flex-start', gap: 10,
      }}>
        <div style={{ color: '#F59E0B', flexShrink: 0, marginTop: 1 }}><Icon.Warn width={16} height={16}/></div>
        <div style={{ fontSize: 12.5, lineHeight: 1.45, color: 'var(--text-2)' }}>
          <strong style={{ color: '#FCD34D' }}>Nearby metal detected.</strong> Bearing may be off by ±18°. Step away from devices, table legs, or rebar.
        </div>
      </div>

      <CompassDisk azimuth={42} qibla={295} size={258} accuracy="low"/>
      <div style={{ marginTop: 12 }}>
        <BearingReadout qibla={295} distance="6,420 km"/>
      </div>

      <div style={{ marginTop: 14, display: 'flex', gap: 8, width: '100%' }}>
        <div style={{
          flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          padding: '10px 12px', background: 'rgba(255,255,255,0.03)',
          border: '1px solid var(--hairline)', borderRadius: 12, fontSize: 12.5,
        }}>
          <span style={{ display: 'flex', alignItems: 'center', gap: 6, color: 'var(--text-3)' }}>
            <Icon.Magnet width={13} height={13}/> Magnetic field
          </span>
          <span style={{ color: '#FCD34D', fontWeight: 700 }}>78µT <span style={{ color: 'var(--text-4)', fontWeight: 500 }}>(normal 25–65)</span></span>
        </div>
      </div>

      <div style={{ flex: 1 }}/>
      <div style={{ display: 'flex', gap: 10, width: '100%', marginBottom: 12 }}>
        <button className="btn btn-ghost" style={{ flex: 1 }}>
          <Icon.Lock width={16} height={16}/> Pre-set direction
        </button>
        <button className="btn btn-quiet" style={{ flex: 1 }}>
          Recalibrate
        </button>
      </div>
    </CompassShell>
  );
}

// ─────────────────────────────────────────────────────────────
// 6. Severe magnetic interference — bearing hidden
// ─────────────────────────────────────────────────────────────
function CompassInterferenceSevere() {
  return (
    <CompassShell accuracy="bad">
      <div style={{ marginTop: 14, position: 'relative' }}>
        <div style={{ filter: 'blur(4px) saturate(0.4)' }}>
          <CompassDisk azimuth={42} qibla={295} size={258} accuracy="unreliable" dim/>
        </div>
        {/* overlay */}
        <div style={{
          position: 'absolute', inset: 0,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <div style={{
            width: 84, height: 84, borderRadius: 999,
            background: 'rgba(239,68,68,0.16)', border: '1.5px solid rgba(239,68,68,0.45)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            color: '#FCA5A5',
          }}>
            <Icon.X width={40} height={40} strokeWidth={2.2}/>
          </div>
        </div>
      </div>

      <div style={{ marginTop: 14, padding: '14px 16px',
        background: 'rgba(239,68,68,0.10)', border: '1px solid rgba(239,68,68,0.30)',
        borderRadius: 14, width: '100%',
      }}>
        <div style={{ fontSize: 15, fontWeight: 800, color: '#FCA5A5', marginBottom: 4 }}>
          Reading not trustworthy
        </div>
        <div style={{ fontSize: 12.5, lineHeight: 1.55, color: 'var(--text-2)' }}>
          Magnetic field is <strong style={{ color: '#FCA5A5' }}>142µT</strong> — way above normal. You're likely near a speaker, laptop, MRI, or steel beam. Move 2 meters away and recalibrate.
        </div>
      </div>

      <div style={{ flex: 1 }}/>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8, width: '100%', marginBottom: 12 }}>
        <button className="btn btn-primary" style={{ width: '100%' }}>
          <Icon.Sparkle width={16} height={16}/> Recalibrate now
        </button>
        <button className="btn btn-ghost" style={{ width: '100%' }}>
          Use pre-set direction instead
        </button>
      </div>
    </CompassShell>
  );
}

// ─────────────────────────────────────────────────────────────
// 7. Locked / using pre-set direction
// ─────────────────────────────────────────────────────────────
function CompassLocked() {
  return (
    <CompassShell accuracy="good" locationLabel="Pre-set · Saved at home">
      {/* Locked banner */}
      <div style={{
        width: '100%', marginBottom: 8, padding: '10px 12px',
        background: 'rgba(34,197,94,0.10)', border: '1px solid rgba(34,197,94,0.30)',
        borderRadius: 12, display: 'flex', alignItems: 'center', gap: 10,
      }}>
        <div style={{ color: '#22c55e', flexShrink: 0 }}><Icon.Lock width={16} height={16}/></div>
        <div style={{ flex: 1, fontSize: 12.5, lineHeight: 1.4, color: 'var(--text-2)' }}>
          Using your <strong style={{ color: '#86EFAC' }}>saved direction</strong>. GPS-free, immune to local interference.
        </div>
        <span style={{ fontSize: 11, color: 'var(--text-4)', fontWeight: 600 }}>2h ago</span>
      </div>

      <div style={{ marginTop: 8 }}>
        <CompassDisk azimuth={42} qibla={295} size={266} accuracy="high" locked/>
      </div>
      <div style={{ marginTop: 14 }}>
        <BearingReadout qibla={295} distance="6,420 km"/>
      </div>

      <div style={{ flex: 1 }}/>
      <div style={{ display: 'flex', gap: 10, width: '100%', marginBottom: 12 }}>
        <button className="btn btn-quiet" style={{ flex: 1 }}>
          <Icon.Unlock width={16} height={16}/> Use live compass
        </button>
        <button className="btn btn-ghost" style={{ flex: 1 }}>
          Re-anchor
        </button>
      </div>
    </CompassShell>
  );
}

Object.assign(window, {
  CompassFindingLocation, CompassPoseHint, CompassCalibrating,
  CompassReady, CompassInterferenceMild, CompassInterferenceSevere, CompassLocked,
});
