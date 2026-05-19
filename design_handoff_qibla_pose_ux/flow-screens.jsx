// Lock / pre-set flow — 3 steps showing how a user saves a known-good
// Qibla bearing for later use when GPS or compass are unreliable.

// ─────────────────────────────────────────────────────────────
// Step 1: Why pre-set? — explainer modal
// ─────────────────────────────────────────────────────────────
function LockStep1() {
  return (
    <div className="phone-bg" style={{ background: 'var(--bg-app)', display: 'flex', flexDirection: 'column', position: 'relative' }}>
      {/* faded compass behind */}
      <div style={{ position: 'absolute', inset: 0, opacity: 0.18, filter: 'blur(2px)' }}>
        <div style={{ position: 'absolute', left: '50%', top: '22%', transform: 'translateX(-50%)' }}>
          <CompassDisk azimuth={42} qibla={295} size={300} accuracy="high"/>
        </div>
      </div>
      <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(180deg, rgba(20,21,42,0.4), rgba(20,21,42,0.92))' }}/>

      <div style={{ position: 'absolute', left: 16, top: 16 }}>
        <button style={{
          width: 36, height: 36, borderRadius: 999,
          background: 'rgba(255,255,255,0.06)', border: '1px solid var(--hairline)',
          color: 'var(--text)', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer',
        }}>
          <Icon.X width={18} height={18}/>
        </button>
      </div>

      <div style={{ flex: 1, position: 'relative', zIndex: 2, display: 'flex', flexDirection: 'column', justifyContent: 'flex-end', padding: 24 }}>
        <div style={{
          width: 64, height: 64, borderRadius: 18,
          background: 'rgba(34,197,94,0.16)', border: '1px solid rgba(34,197,94,0.35)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          color: '#22c55e', marginBottom: 18,
        }}>
          <Icon.Lock width={28} height={28} strokeWidth={2}/>
        </div>
        <div style={{ fontSize: 28, fontWeight: 800, lineHeight: 1.15, letterSpacing: -0.015 }}>
          Save your Qibla<br/>direction for later
        </div>
        <div style={{ marginTop: 12, fontSize: 14.5, color: 'var(--text-2)', lineHeight: 1.55 }}>
          Pre-set the bearing while you have good GPS — then pray anywhere, even with no signal or magnetic interference.
        </div>

        <div style={{ marginTop: 22, display: 'flex', flexDirection: 'column', gap: 10 }}>
          <Benefit
            icon={<Icon.GPS width={16} height={16}/>} color="#22c55e"
            title="Works without GPS"
            body="Saved direction follows you via gyroscope only"
          />
          <Benefit
            icon={<Icon.Magnet width={16} height={16}/>} color="#22c55e"
            title="Immune to metal & electronics"
            body="The magnetic field at the prayer spot can't fool it"
          />
          <Benefit
            icon={<Icon.Compass width={16} height={16}/>} color="#22c55e"
            title="Quick to recall"
            body="Open the app and the Kaaba marker is already there"
          />
        </div>

        <button className="btn btn-primary" style={{ marginTop: 26, width: '100%' }}>
          Continue <Icon.ChevronRight width={16} height={16}/>
        </button>
        <button className="btn btn-link" style={{ marginTop: 4, alignSelf: 'center' }}>Maybe later</button>
      </div>
    </div>
  );
}

function Benefit({ icon, color, title, body }) {
  return (
    <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12 }}>
      <div style={{
        width: 32, height: 32, borderRadius: 10, flexShrink: 0,
        background: `${color}22`, color,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}>{icon}</div>
      <div>
        <div style={{ fontSize: 14, fontWeight: 700, color: 'var(--text)' }}>{title}</div>
        <div style={{ fontSize: 12.5, color: 'var(--text-3)', marginTop: 1, lineHeight: 1.4 }}>{body}</div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Step 2: Choose method — capture current OR set manually
// ─────────────────────────────────────────────────────────────
function LockStep2() {
  return (
    <div className="phone-bg" style={{ background: 'var(--bg-app)', display: 'flex', flexDirection: 'column', position: 'relative' }}>
      {/* Top bar */}
      <div style={{ padding: '12px 16px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <button style={{
          width: 36, height: 36, borderRadius: 999,
          background: 'transparent', border: 'none', color: 'var(--text)', cursor: 'pointer',
        }} aria-label="Back">←</button>
        <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--text-3)', letterSpacing: 0.8 }}>STEP 2 OF 3</div>
        <div style={{ width: 36 }}/>
      </div>

      <div style={{ flex: 1, padding: '0 20px', display: 'flex', flexDirection: 'column' }}>
        <div style={{ fontSize: 24, fontWeight: 800, letterSpacing: -0.01 }}>
          How would you like<br/>to set it?
        </div>
        <div style={{ marginTop: 8, fontSize: 13.5, color: 'var(--text-3)', lineHeight: 1.5 }}>
          Pick the most accurate method available right now.
        </div>

        {/* Option A — Capture current (recommended) */}
        <div style={{
          marginTop: 22, padding: 18, borderRadius: 18,
          background: 'rgba(34,197,94,0.08)',
          border: '1.5px solid rgba(34,197,94,0.45)',
          position: 'relative',
        }}>
          <div style={{
            position: 'absolute', top: 14, right: 14,
            padding: '3px 8px', borderRadius: 999,
            background: '#22c55e', color: '#042814',
            fontSize: 10, fontWeight: 800, letterSpacing: 0.5,
          }}>RECOMMENDED</div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div style={{
              width: 44, height: 44, borderRadius: 12,
              background: 'rgba(34,197,94,0.2)', color: '#22c55e',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              <Icon.GPS width={22} height={22}/>
            </div>
            <div>
              <div style={{ fontSize: 16, fontWeight: 800, color: 'var(--text)' }}>Capture current direction</div>
              <div style={{ fontSize: 12, color: 'var(--text-3)', marginTop: 1 }}>
                Uses your GPS + calibrated compass right now
              </div>
            </div>
          </div>
          {/* Live readiness preview */}
          <div style={{ marginTop: 12, display: 'flex', gap: 6 }}>
            <ReadyTag tone="green">GPS ±3m</ReadyTag>
            <ReadyTag tone="green">Compass ±5°</ReadyTag>
            <ReadyTag tone="green">Magnetic OK</ReadyTag>
          </div>
          <button className="btn btn-primary" style={{ marginTop: 14, width: '100%' }}>
            Lay phone flat & tap to capture
          </button>
        </div>

        {/* Option B — Manual */}
        <div style={{
          marginTop: 12, padding: 18, borderRadius: 18,
          background: 'rgba(255,255,255,0.03)',
          border: '1px solid var(--hairline)',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div style={{
              width: 44, height: 44, borderRadius: 12,
              background: 'rgba(245,158,11,0.15)', color: '#F59E0B',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              <Icon.Compass width={22} height={22}/>
            </div>
            <div>
              <div style={{ fontSize: 16, fontWeight: 800, color: 'var(--text)' }}>Set manually</div>
              <div style={{ fontSize: 12, color: 'var(--text-3)', marginTop: 1 }}>
                Align with a prayer rug or mosque mihrab you know
              </div>
            </div>
          </div>
          <div style={{ marginTop: 12, fontSize: 12, color: 'var(--text-3)', lineHeight: 1.5 }}>
            Best when you can see a trusted reference — a mosque's mihrab, an existing musolla, or a marked carpet line.
          </div>
        </div>

        <div style={{ flex: 1 }}/>
        <div style={{
          padding: '10px 12px', marginBottom: 16,
          background: 'rgba(255,255,255,0.03)', border: '1px solid var(--hairline)',
          borderRadius: 12, display: 'flex', alignItems: 'flex-start', gap: 10,
        }}>
          <Icon.Info width={15} height={15} style={{ color: 'var(--text-3)', flexShrink: 0, marginTop: 1 }}/>
          <div style={{ fontSize: 11.5, color: 'var(--text-3)', lineHeight: 1.45 }}>
            Either way, the saved direction is anchored to the gyroscope — it follows you when you move, even in basements or steel buildings.
          </div>
        </div>
      </div>
    </div>
  );
}

function ReadyTag({ tone='green', children }) {
  const colors = tone === 'green'
    ? { bg: 'rgba(34,197,94,0.18)', fg: '#86EFAC' }
    : { bg: 'rgba(245,158,11,0.18)', fg: '#FCD34D' };
  return (
    <span style={{
      fontSize: 10.5, fontWeight: 700, padding: '4px 8px', borderRadius: 999,
      background: colors.bg, color: colors.fg, letterSpacing: 0.2,
    }}>{children}</span>
  );
}

// ─────────────────────────────────────────────────────────────
// Step 3: Confirmation — direction saved
// ─────────────────────────────────────────────────────────────
function LockStep3() {
  return (
    <div className="phone-bg" style={{ background: 'var(--bg-app)', display: 'flex', flexDirection: 'column', position: 'relative' }}>
      {/* subtle green wash */}
      <div style={{
        position: 'absolute', inset: 0,
        background: 'radial-gradient(ellipse at 50% 30%, rgba(34,197,94,0.18), transparent 60%)',
      }}/>

      <div style={{ position: 'relative', zIndex: 1, flex: 1, padding: 24, display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center' }}>
        <div style={{ height: 36 }}/>
        {/* Success badge */}
        <div style={{ position: 'relative', width: 120, height: 120, marginTop: 20 }}>
          <div style={{
            position: 'absolute', inset: 0,
            background: 'radial-gradient(circle, rgba(34,197,94,0.4), transparent 70%)',
          }}/>
          <div style={{
            position: 'absolute', inset: 10, borderRadius: 999,
            background: 'rgba(34,197,94,0.18)',
            border: '1.5px solid rgba(34,197,94,0.5)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            color: '#22c55e',
          }}>
            <Icon.Check width={50} height={50} strokeWidth={2.4}/>
          </div>
        </div>

        <div style={{ marginTop: 22, fontSize: 26, fontWeight: 800, letterSpacing: -0.01 }}>
          Direction saved
        </div>
        <div style={{ marginTop: 8, fontSize: 14, color: 'var(--text-2)', lineHeight: 1.55, maxWidth: 300 }}>
          Your Qibla is anchored to the gyroscope. Tracks accurately even without GPS or compass.
        </div>

        {/* Saved bearing card */}
        <div style={{
          marginTop: 24, width: '100%', padding: 18, borderRadius: 18,
          background: 'rgba(255,255,255,0.04)', border: '1px solid var(--hairline)',
          display: 'flex', flexDirection: 'column', gap: 14,
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <div style={{
                width: 36, height: 36, borderRadius: 10, background: 'rgba(34,197,94,0.18)', color: '#22c55e',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}><Icon.Lock width={18} height={18}/></div>
              <div style={{ textAlign: 'left' }}>
                <div style={{ fontSize: 11, color: 'var(--text-4)', fontWeight: 700, letterSpacing: 0.5, textTransform: 'uppercase' }}>Saved bearing</div>
                <div style={{ fontSize: 22, fontWeight: 800, fontVariantNumeric: 'tabular-nums', color: 'var(--text)', marginTop: -2 }}>295° NW</div>
              </div>
            </div>
            <div style={{ textAlign: 'right' }}>
              <div style={{ fontSize: 11, color: 'var(--text-4)', fontWeight: 700, letterSpacing: 0.5, textTransform: 'uppercase' }}>From</div>
              <div style={{ fontSize: 13, fontWeight: 700, color: 'var(--text)', marginTop: 2 }}>Tampines, SG</div>
            </div>
          </div>
          <div className="hairline"/>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: 'var(--text-3)' }}>
            <span>Distance to Kaaba</span>
            <span style={{ color: 'var(--text), fontWeight: 600' }}>6,420 km</span>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: 'var(--text-3)' }}>
            <span>Captured</span>
            <span style={{ color: 'var(--text)', fontWeight: 600 }}>Just now · ±5°</span>
          </div>
        </div>

        <div style={{ flex: 1 }}/>
        <div style={{ width: '100%', display: 'flex', flexDirection: 'column', gap: 8 }}>
          <button className="btn btn-primary" style={{ width: '100%' }}>
            Open in AR view
          </button>
          <button className="btn btn-ghost" style={{ width: '100%' }}>
            Back to compass
          </button>
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Calibration coachmark — figure-8 with phone path & progress
// (Lives on the "Calibrate" tab; users land here from accuracy warnings.)
// ─────────────────────────────────────────────────────────────
function CalibrationScreen() {
  return (
    <div className="phone-bg" style={{ background: 'var(--bg-app)', display: 'flex', flexDirection: 'column' }}>
      <div style={{ padding: '12px 20px 0', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ fontSize: 11, color: 'var(--text-4)', fontWeight: 600, letterSpacing: 0.6, textTransform: 'uppercase' }}>Step 1 of 2 · Compass</div>
        <span className="chip chip-amber"><Dot color="#F59E0B"/>Needs calibration</span>
      </div>

      <div style={{ flex: 1, padding: '12px 20px', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        <div style={{ marginTop: 8, fontSize: 24, fontWeight: 800, color: 'var(--text)', letterSpacing: -0.01, textAlign: 'center' }}>
          Trace a figure-8
        </div>
        <div style={{ marginTop: 6, fontSize: 13.5, color: 'var(--text-2)', textAlign: 'center', maxWidth: 290, lineHeight: 1.5 }}>
          Hold your phone in front of you and draw a slow 8 through the air. The compass learns from how it tumbles.
        </div>

        {/* Big figure-8 illustration */}
        <div style={{ marginTop: 22 }}>
          <BigFigure8 progress={0.62}/>
        </div>

        {/* progress + ETA */}
        <div style={{ marginTop: 18, width: '100%', maxWidth: 280 }}>
          <div style={{
            height: 6, borderRadius: 999, background: 'rgba(255,255,255,0.08)', overflow: 'hidden',
          }}>
            <div style={{ width: '62%', height: '100%', background: 'linear-gradient(90deg, #F59E0B, #22c55e)' }}/>
          </div>
          <div style={{ marginTop: 6, display: 'flex', justifyContent: 'space-between', fontSize: 12, color: 'var(--text-3)', fontWeight: 600 }}>
            <span>Strength medium</span>
            <span style={{ color: '#22c55e' }}>~3 seconds left</span>
          </div>
        </div>

        {/* Tips */}
        <div style={{
          marginTop: 22, width: '100%', padding: 14, borderRadius: 14,
          background: 'rgba(255,255,255,0.03)', border: '1px solid var(--hairline)',
        }}>
          <div style={{ fontSize: 12, fontWeight: 800, color: 'var(--text-2)', letterSpacing: 0.4, textTransform: 'uppercase', marginBottom: 8 }}>
            For best accuracy
          </div>
          <Tip icon={<Icon.Magnet width={14} height={14}/>} text="Move away from laptops, speakers, and metal furniture"/>
          <Tip icon={<Icon.Sparkle width={14} height={14}/>} text="Twist the phone — not just slide it side to side"/>
          <Tip icon={<Icon.Compass width={14} height={14}/>} text="Repeat any time the compass jumps suddenly"/>
        </div>
      </div>

      <BottomNav active="calibrate"/>
    </div>
  );
}

function Tip({ icon, text }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '5px 0' }}>
      <div style={{ color: 'var(--text-3)', flexShrink: 0 }}>{icon}</div>
      <div style={{ fontSize: 12.5, color: 'var(--text-2)', lineHeight: 1.4 }}>{text}</div>
    </div>
  );
}

function BigFigure8({ progress = 0.5 }) {
  // total path roughly approximated by drawing two ellipses joined.
  // We animate a moving dot along a synthesized parametric path.
  const t = progress;
  // Lissajous-style figure 8: x = A sin(t), y = B sin(2t)
  const A = 80, B = 50;
  const theta = t * Math.PI * 2;
  const x = 130 + A * Math.sin(theta);
  const y = 80  + B * Math.sin(2 * theta);

  return (
    <svg width="260" height="160" viewBox="0 0 260 160">
      <defs>
        <linearGradient id="bigFigGrad" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0%" stopColor="#22c55e" stopOpacity="0"/>
          <stop offset="50%" stopColor="#22c55e"/>
          <stop offset="100%" stopColor="#F59E0B"/>
        </linearGradient>
        <filter id="glow" x="-50%" y="-50%" width="200%" height="200%">
          <feGaussianBlur stdDeviation="3"/>
        </filter>
      </defs>
      {/* dashed guide path approximation */}
      <path d="M50 80 C 50 20, 130 20, 130 80 C 130 140, 210 140, 210 80 C 210 20, 130 20, 130 80 C 130 140, 50 140, 50 80 Z"
        fill="none" stroke="rgba(255,255,255,0.15)" strokeWidth="2.5" strokeDasharray="5 7"/>
      {/* progress overlay */}
      <path d="M50 80 C 50 20, 130 20, 130 80 C 130 140, 210 140, 210 80"
        fill="none" stroke="url(#bigFigGrad)" strokeWidth="4" strokeLinecap="round"/>
      {/* moving dot */}
      <circle cx={x} cy={y} r="14" fill="#22c55e" opacity="0.25" filter="url(#glow)"/>
      <circle cx={x} cy={y} r="7" fill="#22c55e"/>
      <circle cx={x} cy={y} r="3" fill="#fff"/>
    </svg>
  );
}

Object.assign(window, { LockStep1, LockStep2, LockStep3, CalibrationScreen });
