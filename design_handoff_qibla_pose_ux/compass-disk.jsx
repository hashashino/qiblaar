// Compass disc — beautiful dark dial with cardinals + Qibla pointer.
// Props:
//   azimuth: degrees the phone is pointing (0=N, 90=E)
//   qibla: degrees from true north toward Kaaba
//   size: pixel diameter
//   accuracy: 'high' | 'low' | 'unreliable'  (controls saturation/dimming)
//   locked: bool                              (shows lock badge in center)
//   dim: bool                                 (severe interference / wrong pose)

function CompassDisk({
  azimuth = 0, qibla = 295, size = 280,
  accuracy = 'high', locked = false, dim = false,
  showDegrees = true,
  centerLabel,
}) {
  const r = size / 2;
  const ringR  = r * 0.93;
  const tickOuter = r * 0.86;
  const opacityScale = dim ? 0.35 : (accuracy === 'unreliable' ? 0.55 : 1);

  // ticks every 5°
  const ticks = [];
  for (let i = 0; i < 360; i += 5) {
    const major = (i % 90) === 0;
    const med   = (i % 30) === 0;
    const small = (i % 10) === 0;
    const len = major ? r * 0.16 : med ? r * 0.12 : small ? r * 0.07 : r * 0.04;
    const sw = major ? 2.5 : med ? 1.8 : 1;
    const color = major ? '#ffffff' : small ? 'rgba(255,255,255,0.6)' : 'rgba(255,255,255,0.28)';
    const rad = (i - 90) * Math.PI / 180;
    const x1 = r + Math.cos(rad) * tickOuter;
    const y1 = r + Math.sin(rad) * tickOuter;
    const x2 = r + Math.cos(rad) * (tickOuter - len);
    const y2 = r + Math.sin(rad) * (tickOuter - len);
    ticks.push(<line key={i} x1={x1} y1={y1} x2={x2} y2={y2} stroke={color} strokeWidth={sw} strokeLinecap="round"/>);
  }

  // cardinals
  const cardinals = [
    { l: 'N', deg: 0,   color: '#EF4444', main: true },
    { l: 'E', deg: 90,  color: '#fff' },
    { l: 'S', deg: 180, color: '#fff' },
    { l: 'W', deg: 270, color: '#fff' },
  ].map(c => {
    const rad = (c.deg - 90) * Math.PI / 180;
    const tr = r * 0.62;
    return (
      <text key={c.l}
        x={r + Math.cos(rad) * tr}
        y={r + Math.sin(rad) * tr}
        fill={c.color}
        fontFamily="'Plus Jakarta Sans', sans-serif"
        fontWeight={c.main ? 800 : 700}
        fontSize={c.main ? 22 : 18}
        textAnchor="middle"
        dominantBaseline="central"
      >{c.l}</text>
    );
  });

  // Qibla arrow — green pointer extending from center outward at angle `qibla`
  // (we rotate the entire dial, so the pointer rotates with cardinals).
  const qibRad = (qibla - 90) * Math.PI / 180;
  const qibTip = r * 0.40;
  const tipX = r + Math.cos(qibRad) * qibTip;
  const tipY = r + Math.sin(qibRad) * qibTip;

  return (
    <div style={{ position: 'relative', width: size, height: size }}>
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}
           style={{ opacity: opacityScale, transition: 'opacity 250ms' }}>
        <defs>
          <radialGradient id="diskGrad" cx="50%" cy="42%" r="62%">
            <stop offset="0%"   stopColor="#22264E" />
            <stop offset="65%"  stopColor="#171935" />
            <stop offset="100%" stopColor="#0E0F22" />
          </radialGradient>
          <radialGradient id="qibGlow" cx="50%" cy="50%" r="50%">
            <stop offset="0%"   stopColor="rgba(34,197,94,0.55)"/>
            <stop offset="60%"  stopColor="rgba(34,197,94,0.12)"/>
            <stop offset="100%" stopColor="rgba(34,197,94,0)"/>
          </radialGradient>
        </defs>

        {/* outer ring */}
        <circle cx={r} cy={r} r={ringR} fill="url(#diskGrad)" stroke="rgba(255,255,255,0.06)" strokeWidth="1"/>

        {/* faint inner ring */}
        <circle cx={r} cy={r} r={r * 0.74} fill="none" stroke="rgba(255,255,255,0.05)" />

        {/* The whole dial rotates inversely to the phone azimuth so North always points up */}
        <g transform={`rotate(${-azimuth} ${r} ${r})`}>
          {/* Qibla glow */}
          <circle cx={tipX} cy={tipY} r={r * 0.42} fill="url(#qibGlow)" />

          {ticks}
          {cardinals}

          {/* Qibla arrow */}
          <g>
            <line x1={r} y1={r} x2={tipX} y2={tipY}
              stroke={accuracy === 'high' ? '#22c55e' : '#9CA3AF'}
              strokeWidth="4" strokeLinecap="round"/>
            {/* arrow head triangle */}
            <g transform={`translate(${tipX} ${tipY}) rotate(${qibla})`}>
              <polygon
                points={`0,-${r*0.045} ${r*0.06},${r*0.06} -${r*0.06},${r*0.06}`}
                fill={accuracy === 'high' ? '#22c55e' : '#9CA3AF'} />
            </g>
            {/* Kaaba marker at tip */}
            <g transform={`translate(${tipX} ${tipY}) rotate(${azimuth})`}>
              <circle r={r*0.085} fill="#1B1D38" stroke={accuracy === 'high' ? '#22c55e' : '#9CA3AF'} strokeWidth="1.5"/>
              <g transform={`translate(-7 -7) scale(0.6)`} stroke={accuracy === 'high' ? '#22c55e' : '#9CA3AF'} strokeWidth="2" fill="none" strokeLinejoin="round">
                <rect x="2" y="6" width="20" height="16" rx="0.5"/>
                <path d="M2 11h20M9 6v16M15 6v16"/>
              </g>
            </g>
          </g>

          {/* North needle (red) */}
          <g>
            <polygon
              points={`${r},${r - r*0.74} ${r - r*0.04},${r - r*0.55} ${r + r*0.04},${r - r*0.55}`}
              fill="#EF4444"/>
          </g>
        </g>

        {/* Heading indicator — fixed at top, points DOWN at the disk */}
        <g transform={`translate(${r} ${r * 0.045})`}>
          <polygon points={`0,${r*0.045} -${r*0.05},0 ${r*0.05},0`} fill="#fff"/>
        </g>

        {/* Center hub */}
        <circle cx={r} cy={r} r={r*0.055} fill="#0E0F22" stroke="rgba(255,255,255,0.4)" strokeWidth="1"/>
      </svg>

      {/* Center label overlay */}
      {centerLabel && (
        <div style={{
          position: 'absolute', inset: 0,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          pointerEvents: 'none',
        }}>
          {centerLabel}
        </div>
      )}

      {/* Locked badge */}
      {locked && !centerLabel && (
        <div style={{
          position: 'absolute', top: '50%', left: '50%',
          transform: 'translate(-50%, -50%) translateY(36px)',
          display: 'flex', alignItems: 'center', gap: 5,
          background: 'rgba(34,197,94,0.15)',
          border: '1px solid rgba(34,197,94,0.35)',
          color: '#6EE7A0',
          padding: '4px 10px', borderRadius: 999,
          fontSize: 10.5, fontWeight: 700, letterSpacing: 0.04,
        }}>
          <Icon.Lock width={11} height={11} strokeWidth={2.5}/>
          PRE-SET DIRECTION
        </div>
      )}
    </div>
  );
}

// Big bearing readout under the compass
function BearingReadout({ qibla = 295, distance = '6,420 km', faded = false }) {
  return (
    <div style={{
      display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4,
      opacity: faded ? 0.5 : 1,
    }}>
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 6 }}>
        <span className="num-big" style={{ fontSize: 38, color: '#fff' }}>{Math.round(qibla)}°</span>
        <span style={{ fontSize: 14, color: 'var(--text-3)', fontWeight: 600 }}>true north</span>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: 'var(--text-3)', fontSize: 12.5, fontWeight: 600 }}>
        <Icon.Kaaba width={13} height={13} strokeWidth={2}/>
        <span>Kaaba · {distance} away</span>
      </div>
    </div>
  );
}

Object.assign(window, { CompassDisk, BearingReadout });
