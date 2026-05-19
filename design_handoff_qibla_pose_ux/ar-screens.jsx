// AR screens — all 5 states.
// Each screen mocks the camera feed with a stylized "room" SVG so the
// AR overlay sits on top of something believable.

// ─────────────────────────────────────────────────────────────
// Mocked camera feed — a dim interior with a wall
// ─────────────────────────────────────────────────────────────
function CameraFeed({ angle = 0 }) {
  // angle: how far the user has rotated relative to "facing wall".
  // We shift the room horizontally a bit to suggest sweep.
  const shift = Math.max(-40, Math.min(40, angle * 0.6));
  return (
    <div style={{
      position: 'absolute', inset: 0, background: '#0A0A14',
      overflow: 'hidden',
    }}>
      {/* Vignette */}
      <svg width="100%" height="100%" viewBox="0 0 412 880" preserveAspectRatio="xMidYMid slice" style={{ position: 'absolute', inset: 0 }}>
        <defs>
          <linearGradient id="wall" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#2A2C42"/>
            <stop offset="50%" stopColor="#1E2036"/>
            <stop offset="100%" stopColor="#13152A"/>
          </linearGradient>
          <linearGradient id="floor" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#16182D"/>
            <stop offset="100%" stopColor="#0A0A18"/>
          </linearGradient>
          <radialGradient id="lampSpot" cx="50%" cy="0%" r="60%">
            <stop offset="0%" stopColor="rgba(255,210,140,0.18)"/>
            <stop offset="100%" stopColor="rgba(255,210,140,0)"/>
          </radialGradient>
          <radialGradient id="vignette" cx="50%" cy="50%" r="75%">
            <stop offset="60%" stopColor="rgba(0,0,0,0)"/>
            <stop offset="100%" stopColor="rgba(0,0,0,0.65)"/>
          </radialGradient>
        </defs>

        <g transform={`translate(${shift} 0)`}>
          {/* Wall + floor */}
          <rect x="-100" y="0"   width="612" height="640" fill="url(#wall)"/>
          <rect x="-100" y="640" width="612" height="260" fill="url(#floor)"/>
          {/* Floor perspective lines */}
          <path d="M-100 640 L 206 740 L 512 640 Z" fill="rgba(255,255,255,0.02)"/>
          <line x1="0" y1="640" x2="160" y2="880" stroke="rgba(255,255,255,0.04)" strokeWidth="1"/>
          <line x1="412" y1="640" x2="252" y2="880" stroke="rgba(255,255,255,0.04)" strokeWidth="1"/>

          {/* Wall art — picture frame, gives the AR pose a real-world anchor */}
          <g transform="translate(140 240)">
            <rect x="0" y="0" width="132" height="180" fill="rgba(255,255,255,0.04)" stroke="rgba(255,255,255,0.10)" strokeWidth="1.5"/>
            <rect x="14" y="14" width="104" height="152" fill="rgba(255,255,255,0.02)"/>
          </g>
          {/* lamp spot */}
          <rect x="0" y="0" width="412" height="500" fill="url(#lampSpot)"/>
          {/* Skirting board */}
          <rect x="-100" y="630" width="612" height="14" fill="rgba(255,255,255,0.025)"/>
        </g>

        <rect x="0" y="0" width="412" height="880" fill="url(#vignette)"/>
      </svg>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Top heads-up bar inside AR
// ─────────────────────────────────────────────────────────────
function ArHeader({ status, mode = 'live', headingDeg = 295 }) {
  // status: { tone: 'green'|'amber'|'red', label }
  return (
    <div style={{
      position: 'absolute', top: 0, left: 0, right: 0,
      padding: '8px 16px 0',
      display: 'flex', justifyContent: 'space-between', alignItems: 'center',
      zIndex: 10,
    }}>
      <div style={{
        display: 'flex', alignItems: 'center', gap: 6,
        padding: '6px 10px', borderRadius: 999,
        background: 'rgba(10,12,28,0.7)', backdropFilter: 'blur(8px)',
        border: '1px solid rgba(255,255,255,0.08)',
      }}>
        <Icon.Camera width={13} height={13} style={{ color: 'var(--text-2)' }}/>
        <span style={{ fontSize: 11, fontWeight: 700, color: 'var(--text-2)', letterSpacing: 0.5, textTransform: 'uppercase' }}>
          {mode === 'live' ? 'AR · live' : 'AR · pre-set'}
        </span>
      </div>
      <div style={{
        display: 'flex', alignItems: 'center', gap: 6,
        padding: '6px 10px', borderRadius: 999,
        background: 'rgba(10,12,28,0.7)', backdropFilter: 'blur(8px)',
        border: `1px solid ${status.tone === 'green' ? 'rgba(34,197,94,0.4)'
                          : status.tone === 'amber' ? 'rgba(245,158,11,0.4)'
                          : 'rgba(239,68,68,0.4)'}`,
        color: status.tone === 'green' ? '#86EFAC' : status.tone === 'amber' ? '#FCD34D' : '#FCA5A5',
        fontSize: 11, fontWeight: 700, letterSpacing: 0.4,
      }}>
        {status.label}
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Crosshair / Kaaba marker overlay
// ─────────────────────────────────────────────────────────────
function KaabaMarker({ aligned = false, x = '50%', y = '40%' }) {
  const color = aligned ? '#22c55e' : '#fff';
  return (
    <div style={{
      position: 'absolute', left: x, top: y, transform: 'translate(-50%, -50%)',
      pointerEvents: 'none',
    }}>
      {/* Crosshair brackets */}
      <svg width="180" height="180" viewBox="0 0 180 180">
        <defs>
          <radialGradient id="markerGlow" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stopColor={aligned ? 'rgba(34,197,94,0.45)' : 'rgba(255,255,255,0.15)'}/>
            <stop offset="80%" stopColor={aligned ? 'rgba(34,197,94,0)' : 'rgba(255,255,255,0)'}/>
          </radialGradient>
        </defs>
        <circle cx="90" cy="90" r="90" fill="url(#markerGlow)"/>
        {/* Corner brackets */}
        {[
          'M40 60 L 40 40 L 60 40',
          'M120 40 L 140 40 L 140 60',
          'M40 120 L 40 140 L 60 140',
          'M120 140 L 140 140 L 140 120',
        ].map((d, i) => (
          <path key={i} d={d} stroke={color} strokeWidth="2.5" fill="none" strokeLinecap="round" strokeLinejoin="round"/>
        ))}
        {/* center kaaba */}
        <g transform="translate(90 90)">
          <circle r="22" fill="rgba(10,12,28,0.85)" stroke={color} strokeWidth="1.5"/>
          <g transform="translate(-9 -9)" stroke={color} strokeWidth="1.8" fill="none" strokeLinejoin="round" strokeLinecap="round">
            <rect x="0.5" y="3" width="17" height="13" rx="0.4"/>
            <path d="M0.5 7H17.5M6 3v13M12 3v13"/>
          </g>
        </g>
      </svg>
      {/* Distance label below */}
      <div style={{
        position: 'absolute', left: '50%', top: '100%',
        transform: 'translateX(-50%) translateY(-10px)',
        background: aligned ? 'rgba(34,197,94,0.18)' : 'rgba(10,12,28,0.7)',
        border: `1px solid ${aligned ? 'rgba(34,197,94,0.45)' : 'rgba(255,255,255,0.10)'}`,
        backdropFilter: 'blur(6px)',
        padding: '6px 12px', borderRadius: 999, whiteSpace: 'nowrap',
        fontSize: 11.5, fontWeight: 700, color: aligned ? '#86EFAC' : '#fff', letterSpacing: 0.3,
      }}>
        ﻿ﺍﻟْﻜَﻌْﺒَﺔُ · 6,420 km
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Sweep arrow — tells user which way to turn
// ─────────────────────────────────────────────────────────────
function SweepArrow({ direction = 'right', degrees = 47 }) {
  const flip = direction === 'left';
  return (
    <div style={{
      position: 'absolute',
      top: '42%',
      [flip ? 'left' : 'right']: 24,
      transform: 'translateY(-50%)',
      display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 10,
    }}>
      <svg width="120" height="120" viewBox="0 0 120 120"
        style={{ transform: flip ? 'scaleX(-1)' : 'none' }}>
        <defs>
          <linearGradient id="arrowGrad" x1="0" y1="0" x2="1" y2="0">
            <stop offset="0%" stopColor="rgba(34,197,94,0)"/>
            <stop offset="100%" stopColor="rgba(34,197,94,1)"/>
          </linearGradient>
        </defs>
        <path d="M20 60 L 90 60" stroke="url(#arrowGrad)" strokeWidth="10" strokeLinecap="round"/>
        <path d="M90 60 L 70 40 M90 60 L 70 80" stroke="#22c55e" strokeWidth="10" strokeLinecap="round" strokeLinejoin="round"/>
      </svg>
      <div style={{
        padding: '8px 14px', borderRadius: 999,
        background: 'rgba(34,197,94,0.18)',
        border: '1px solid rgba(34,197,94,0.4)',
        backdropFilter: 'blur(6px)',
        fontSize: 13, fontWeight: 800, color: '#86EFAC', letterSpacing: 0.2,
      }}>
        {flip ? '← Turn left' : 'Turn right →'} {degrees}°
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// 1. AR — wrong pose (phone tilted too flat)
// ─────────────────────────────────────────────────────────────
function ArPoseHint() {
  return (
    <div className="phone-bg" style={{ background: '#0A0A14', display: 'flex', flexDirection: 'column', position: 'relative' }}>
      <CameraFeed/>
      <ArHeader status={{ tone: 'amber', label: 'WRONG POSE' }} />

      {/* Frosted card with pose instruction */}
      <div style={{
        position: 'absolute', left: 20, right: 20, top: '32%',
        padding: 20, borderRadius: 22,
        background: 'rgba(10,12,28,0.78)', backdropFilter: 'blur(14px)',
        border: '1px solid rgba(255,255,255,0.08)',
        display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center', gap: 10,
      }}>
        <PhoneUprightIllustration />
        <div style={{ fontSize: 20, fontWeight: 800, letterSpacing: -0.01 }}>
          Hold phone upright
        </div>
        <div style={{ fontSize: 13.5, color: 'var(--text-2)', lineHeight: 1.5, padding: '0 4px' }}>
          Point the camera at the wall in front of you, like you're taking a portrait photo. The Qibla marker appears where the Kaaba lies through the wall.
        </div>
        <div style={{
          marginTop: 4, padding: '6px 12px',
          background: 'rgba(245,158,11,0.14)', border: '1px solid rgba(245,158,11,0.4)',
          color: '#FCD34D', borderRadius: 999, fontSize: 11.5, fontWeight: 700,
        }}>
          Currently tilted <span style={{ fontVariantNumeric: 'tabular-nums' }}>-68°</span> — needs near vertical
        </div>
      </div>

      {/* bottom controls */}
      <ArBottomBar mode="hint"/>
    </div>
  );
}

// PhoneUprightIllustration lives in pose-illustrations.jsx

// ─────────────────────────────────────────────────────────────
// 2. AR — searching (sweep)
// ─────────────────────────────────────────────────────────────
function ArSearching() {
  return (
    <div className="phone-bg" style={{ background: '#0A0A14', display: 'flex', flexDirection: 'column', position: 'relative' }}>
      <CameraFeed angle={-25}/>
      <ArHeader status={{ tone: 'amber', label: '47° OFF' }} />

      {/* Side hint marker showing the Kaaba is off-screen right */}
      <SweepArrow direction="right" degrees={47}/>

      {/* dim arc on the edge */}
      <div style={{
        position: 'absolute', right: 0, top: 0, bottom: 0, width: 6,
        background: 'linear-gradient(180deg, transparent 30%, rgba(34,197,94,0.5) 50%, transparent 70%)',
      }}/>

      {/* bearing readout */}
      <div style={{
        position: 'absolute', bottom: 110, left: 20, right: 20,
        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
        padding: '10px 14px', borderRadius: 14,
        background: 'rgba(10,12,28,0.8)', backdropFilter: 'blur(10px)',
        border: '1px solid rgba(255,255,255,0.08)',
      }}>
        <div>
          <div style={{ fontSize: 11, color: 'var(--text-4)', fontWeight: 700, letterSpacing: 0.6, textTransform: 'uppercase' }}>Facing</div>
          <div style={{ fontSize: 22, fontWeight: 800, color: 'var(--text)', fontVariantNumeric: 'tabular-nums', marginTop: -2 }}>248°</div>
        </div>
        <Icon.ChevronRight width={20} height={20} style={{ color: 'var(--text-4)' }}/>
        <div style={{ textAlign: 'right' }}>
          <div style={{ fontSize: 11, color: 'var(--text-4)', fontWeight: 700, letterSpacing: 0.6, textTransform: 'uppercase' }}>Kaaba</div>
          <div style={{ fontSize: 22, fontWeight: 800, color: '#22c55e', fontVariantNumeric: 'tabular-nums', marginTop: -2 }}>295°</div>
        </div>
      </div>

      <ArBottomBar mode="searching"/>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// 3. AR — aligned (Kaaba marker centered on wall)
// ─────────────────────────────────────────────────────────────
function ArAligned() {
  return (
    <div className="phone-bg" style={{ background: '#0A0A14', display: 'flex', flexDirection: 'column', position: 'relative' }}>
      <CameraFeed angle={0}/>
      <ArHeader status={{ tone: 'green', label: 'ALIGNED · ±5°' }} />

      <KaabaMarker aligned x="50%" y="38%"/>

      {/* Confirmation toast */}
      <div style={{
        position: 'absolute', left: 20, right: 20, bottom: 130,
        display: 'flex', alignItems: 'center', gap: 12,
        padding: '12px 16px', borderRadius: 16,
        background: 'rgba(34,197,94,0.16)', border: '1px solid rgba(34,197,94,0.40)',
        backdropFilter: 'blur(10px)',
      }}>
        <div style={{
          width: 36, height: 36, borderRadius: 999,
          background: 'rgba(34,197,94,0.3)', color: '#22c55e',
          display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
        }}>
          <Icon.Check width={20} height={20} strokeWidth={3}/>
        </div>
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 14, fontWeight: 800, color: '#86EFAC' }}>Pointing at the Kaaba</div>
          <div style={{ fontSize: 12, color: 'var(--text-2)', marginTop: 1 }}>Lay your prayer mat parallel to this line</div>
        </div>
      </div>

      <ArBottomBar mode="aligned"/>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// 4. AR — magnetic interference (severe)
// ─────────────────────────────────────────────────────────────
function ArInterference() {
  return (
    <div className="phone-bg" style={{ background: '#0A0A14', display: 'flex', flexDirection: 'column', position: 'relative' }}>
      <CameraFeed angle={-10}/>
      <ArHeader status={{ tone: 'red', label: 'COMPASS DISTURBED' }} />

      {/* Drifting / jittery marker — shown faded */}
      <div style={{ filter: 'blur(2px)', opacity: 0.6 }}>
        <KaabaMarker x="58%" y="40%"/>
      </div>

      {/* Big warning card */}
      <div style={{
        position: 'absolute', left: 20, right: 20, top: '32%',
        padding: 18, borderRadius: 20,
        background: 'rgba(10,12,28,0.85)', backdropFilter: 'blur(14px)',
        border: '1px solid rgba(239,68,68,0.40)',
      }}>
        <div style={{ display: 'flex', gap: 12, alignItems: 'flex-start' }}>
          <div style={{
            width: 36, height: 36, borderRadius: 999, flexShrink: 0,
            background: 'rgba(239,68,68,0.18)', color: '#FCA5A5',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <Icon.Magnet width={18} height={18}/>
          </div>
          <div>
            <div style={{ fontSize: 16, fontWeight: 800, color: '#FCA5A5' }}>
              The AR marker is drifting
            </div>
            <div style={{ marginTop: 4, fontSize: 13, color: 'var(--text-2)', lineHeight: 1.5 }}>
              We detected <strong style={{ color: '#fff' }}>142 µT</strong> nearby — likely speakers, laptop, or a metal door frame. Don't trust this bearing.
            </div>
          </div>
        </div>
        <div style={{
          marginTop: 12, padding: '10px 12px',
          background: 'rgba(34,197,94,0.10)', border: '1px solid rgba(34,197,94,0.30)',
          borderRadius: 12, display: 'flex', alignItems: 'center', gap: 10,
        }}>
          <Icon.Lock width={15} height={15} style={{ color: '#22c55e', flexShrink: 0 }}/>
          <span style={{ fontSize: 12.5, color: 'var(--text-2)', lineHeight: 1.4 }}>
            Tip: Switch to your pre-set direction. Gyro-only tracking ignores magnetic noise.
          </span>
        </div>
      </div>

      <ArBottomBar mode="interference"/>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// 5. AR — using pre-set / gyro lock
// ─────────────────────────────────────────────────────────────
function ArLocked() {
  return (
    <div className="phone-bg" style={{ background: '#0A0A14', display: 'flex', flexDirection: 'column', position: 'relative' }}>
      <CameraFeed angle={2}/>
      <ArHeader status={{ tone: 'green', label: 'PRE-SET · ±3°' }} mode="locked"/>

      <KaabaMarker aligned x="52%" y="38%"/>

      {/* Pre-set banner */}
      <div style={{
        position: 'absolute', left: 20, right: 20, bottom: 130,
        padding: '12px 14px', borderRadius: 16,
        background: 'rgba(10,12,28,0.85)', backdropFilter: 'blur(10px)',
        border: '1px solid rgba(34,197,94,0.35)',
        display: 'flex', alignItems: 'center', gap: 12,
      }}>
        <div style={{
          width: 34, height: 34, borderRadius: 999, flexShrink: 0,
          background: 'rgba(34,197,94,0.18)', color: '#22c55e',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <Icon.Lock width={16} height={16}/>
        </div>
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 13.5, fontWeight: 800, color: '#86EFAC' }}>Pre-set direction · gyro lock</div>
          <div style={{ fontSize: 12, color: 'var(--text-2)', marginTop: 1, lineHeight: 1.4 }}>
            Saved at home · won't drift from metal here
          </div>
        </div>
        <button style={{
          background: 'transparent', border: 'none', color: 'var(--text-3)',
          fontSize: 12, fontWeight: 700, padding: 0, cursor: 'pointer',
        }}>Unlock</button>
      </div>

      <ArBottomBar mode="locked"/>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Bottom action bar (shared by all AR screens)
// ─────────────────────────────────────────────────────────────
function ArBottomBar({ mode = 'live' }) {
  return (
    <div style={{
      position: 'absolute', left: 0, right: 0, bottom: 0,
      background: 'linear-gradient(180deg, transparent, rgba(10,12,28,0.95) 30%)',
      paddingTop: 24,
    }}>
      <div style={{ padding: '0 16px 8px', display: 'flex', justifyContent: 'center' }}>
        {mode === 'aligned' && (
          <button className="btn btn-primary" style={{ width: '100%' }}>
            <Icon.Lock width={16} height={16}/> Save this direction
          </button>
        )}
        {mode === 'searching' && (
          <div style={{ fontSize: 12.5, color: 'var(--text-3)', fontWeight: 600, padding: '12px 0' }}>
            Slowly sweep the phone right →
          </div>
        )}
        {mode === 'hint' && (
          <button className="btn btn-quiet" style={{ width: '100%' }}>
            Use compass mode instead
          </button>
        )}
        {mode === 'interference' && (
          <button className="btn btn-primary" style={{ width: '100%' }}>
            <Icon.Lock width={16} height={16}/> Switch to pre-set
          </button>
        )}
        {mode === 'locked' && (
          <div style={{ fontSize: 12, color: 'var(--text-3)', fontWeight: 600, padding: '12px 0' }}>
            Marker tracks via gyroscope only
          </div>
        )}
      </div>
      <BottomNav active="ar"/>
    </div>
  );
}

Object.assign(window, {
  ArPoseHint, ArSearching, ArAligned, ArInterference, ArLocked,
  CameraFeed, ArHeader, KaabaMarker,
});
