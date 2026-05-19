// Qibla AR — shared phone shell + small atomic UI parts.
// Custom dark Android-style frame matching the app's #1A1A2E palette.

const { useState, useEffect, useMemo } = React;

// ─────────────────────────────────────────────────────────────
// Phone shell
// ─────────────────────────────────────────────────────────────
function Phone({ children, dark = true, statusDark = true, label }) {
  return (
    <div style={{
      width: 412, height: 880, borderRadius: 36,
      background: dark ? '#14152A' : '#fff',
      border: '6px solid #2A2C45',
      boxShadow: '0 50px 100px -30px rgba(0,0,0,0.6), 0 0 0 1px rgba(255,255,255,0.04)',
      overflow: 'hidden',
      display: 'flex', flexDirection: 'column',
      position: 'relative',
      fontFamily: "'Plus Jakarta Sans', system-ui, sans-serif",
    }}>
      <PhoneStatusBar dark={statusDark} />
      <div style={{ flex: 1, position: 'relative', overflow: 'hidden' }}>
        {children}
      </div>
      <PhoneNavPill dark={statusDark} />
    </div>
  );
}

function PhoneStatusBar({ dark = true }) {
  const c = dark ? '#fff' : '#0F172A';
  return (
    <div style={{
      height: 36, padding: '0 24px 0 28px',
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      position: 'relative',
      color: c, fontSize: 13, fontWeight: 600,
      flexShrink: 0,
      zIndex: 50,
    }}>
      <span style={{ letterSpacing: 0.2 }}>9:41</span>
      {/* punch hole */}
      <div style={{
        position: 'absolute', top: 10, left: '50%', transform: 'translateX(-50%)',
        width: 16, height: 16, borderRadius: 999, background: '#0A0A14',
      }} />
      <div style={{ display: 'flex', gap: 5, alignItems: 'center' }}>
        {/* signal */}
        <svg width="15" height="11" viewBox="0 0 15 11" fill={c}>
          <rect x="0"  y="7" width="2" height="4" rx="0.5"/>
          <rect x="4"  y="5" width="2" height="6" rx="0.5"/>
          <rect x="8"  y="2.5" width="2" height="8.5" rx="0.5"/>
          <rect x="12" y="0" width="2" height="11" rx="0.5"/>
        </svg>
        {/* wifi */}
        <svg width="14" height="11" viewBox="0 0 14 11" fill={c}>
          <path d="M7 10.5 4.5 8a3.5 3.5 0 0 1 5 0L7 10.5z"/>
          <path d="M7 6 2.5 1.5a6.5 6.5 0 0 1 9 0L7 6Z" opacity=".6"/>
        </svg>
        {/* battery */}
        <svg width="24" height="11" viewBox="0 0 24 11">
          <rect x="0.5" y="0.5" width="21" height="10" rx="2.5" fill="none" stroke={c} opacity=".5"/>
          <rect x="22.5" y="3.5" width="1.5" height="4" rx="0.5" fill={c} opacity=".5"/>
          <rect x="2" y="2" width="15" height="7" rx="1.5" fill={c}/>
        </svg>
      </div>
    </div>
  );
}

function PhoneNavPill({ dark = true }) {
  return (
    <div style={{
      height: 22, display: 'flex', alignItems: 'center', justifyContent: 'center',
      flexShrink: 0, position: 'relative', zIndex: 50,
    }}>
      <div style={{
        width: 132, height: 4, borderRadius: 2,
        background: dark ? 'rgba(255,255,255,0.5)' : 'rgba(0,0,0,0.5)',
      }} />
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Bottom nav (Compass / AR / Calibrate)
// ─────────────────────────────────────────────────────────────
function BottomNav({ active = 'compass' }) {
  const Item = ({ id, label, children }) => (
    <div className={`botnav-item ${active===id ? 'active' : ''}`}>
      {children}
      <span>{label}</span>
    </div>
  );
  return (
    <div className="botnav">
      <Item id="compass" label="Compass">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="12" r="10"/>
          <polygon points="16.24 7.76 14.12 14.12 7.76 16.24 9.88 9.88 16.24 7.76" fill={active==='compass' ? 'currentColor' : 'none'}/>
        </svg>
      </Item>
      <Item id="ar" label="AR View">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M3 7V5a2 2 0 0 1 2-2h2"/>
          <path d="M17 3h2a2 2 0 0 1 2 2v2"/>
          <path d="M21 17v2a2 2 0 0 1-2 2h-2"/>
          <path d="M7 21H5a2 2 0 0 1-2-2v-2"/>
          <circle cx="12" cy="12" r="3"/>
        </svg>
      </Item>
      <Item id="calibrate" label="Calibrate">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M3 12c0-3 2-6 6-6s4 3 4 3-2-3-4-3 4 6 4 9-2 3-4 3-3-2-3-2"/>
          <path d="M21 12c0 3-2 6-6 6s-4-3-4-3"/>
        </svg>
      </Item>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Common atoms
// ─────────────────────────────────────────────────────────────
function Chip({ tone='neutral', icon, children }) {
  const cls = tone === 'green' ? 'chip chip-green'
            : tone === 'amber' ? 'chip chip-amber'
            : tone === 'red'   ? 'chip chip-red'
            : 'chip';
  return (
    <span className={cls}>
      {icon}
      {children}
    </span>
  );
}

// little dot indicator
function Dot({ color='#22c55e' }) {
  return <span style={{ width:6,height:6,borderRadius:999,background:color,display:'inline-block' }} />;
}

// ─────────────────────────────────────────────────────────────
// Icon set (lucide-style stroke icons)
// ─────────────────────────────────────────────────────────────
const stroke = { fill: 'none', stroke: 'currentColor', strokeWidth: 2, strokeLinecap: 'round', strokeLinejoin: 'round' };
const Icon = {
  Lock:   (p) => <svg viewBox="0 0 24 24" {...stroke} {...p}><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>,
  Unlock: (p) => <svg viewBox="0 0 24 24" {...stroke} {...p}><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 9.9-1"/></svg>,
  Kaaba:  (p) => <svg viewBox="0 0 24 24" {...stroke} {...p}><rect x="4" y="6" width="16" height="14" rx="0.5"/><path d="M4 10h16M9 6v14M15 6v14"/></svg>,
  Warn:   (p) => <svg viewBox="0 0 24 24" {...stroke} {...p}><path d="M12 3 2 21h20L12 3Z"/><path d="M12 10v5M12 18v.5"/></svg>,
  Info:   (p) => <svg viewBox="0 0 24 24" {...stroke} {...p}><circle cx="12" cy="12" r="9"/><path d="M12 8v.5M11 12h1v5h1"/></svg>,
  Check:  (p) => <svg viewBox="0 0 24 24" {...stroke} {...p}><path d="m5 12 5 5L20 7"/></svg>,
  ChevronRight: (p) => <svg viewBox="0 0 24 24" {...stroke} {...p}><path d="m9 6 6 6-6 6"/></svg>,
  GPS:    (p) => <svg viewBox="0 0 24 24" {...stroke} {...p}><path d="M12 2v3M12 19v3M2 12h3M19 12h3"/><circle cx="12" cy="12" r="7"/><circle cx="12" cy="12" r="2" fill="currentColor"/></svg>,
  Compass:(p) => <svg viewBox="0 0 24 24" {...stroke} {...p}><circle cx="12" cy="12" r="9"/><path d="m15.5 8.5-2.1 4.9-4.9 2.1 2.1-4.9 4.9-2.1Z"/></svg>,
  Camera: (p) => <svg viewBox="0 0 24 24" {...stroke} {...p}><path d="M3 8a2 2 0 0 1 2-2h2l2-2h6l2 2h2a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8z"/><circle cx="12" cy="13" r="3.5"/></svg>,
  Magnet: (p) => <svg viewBox="0 0 24 24" {...stroke} {...p}><path d="M5 4h4v8a3 3 0 1 0 6 0V4h4v8a7 7 0 1 1-14 0V4Z"/><path d="M5 9h4M15 9h4"/></svg>,
  X:      (p) => <svg viewBox="0 0 24 24" {...stroke} {...p}><path d="M6 6 18 18M18 6 6 18"/></svg>,
  ArrowUp:(p) => <svg viewBox="0 0 24 24" {...stroke} {...p}><path d="M12 19V5M5 12l7-7 7 7"/></svg>,
  RotateCcw: (p) => <svg viewBox="0 0 24 24" {...stroke} {...p}><path d="M3 12a9 9 0 1 0 3-6.7L3 8"/><path d="M3 3v5h5"/></svg>,
  Sparkle:(p) => <svg viewBox="0 0 24 24" {...stroke} {...p}><path d="M12 3v4M12 17v4M3 12h4M17 12h4M5.6 5.6l2.8 2.8M15.6 15.6l2.8 2.8M5.6 18.4l2.8-2.8M15.6 8.4l2.8-2.8"/></svg>,
};

Object.assign(window, { Phone, PhoneStatusBar, PhoneNavPill, BottomNav, Chip, Dot, Icon });
