// Pose illustrations — animated, world-grounded, with directional motion.
// PhoneFlatIllustration: phone starts UPRIGHT on the left, tips down RIGHT to
//   lay flat on the table. Bubble level + mini compass appear when level.
// PhoneUprightIllustration: phone starts HORIZONTAL in hand (screen up, like
//   compass pose), rotates UP to vertical facing the wall. Camera ray casts to
//   a Kaaba marker on the wall when correct.
// Both loop on a 3.6s cycle so the user sees a full cycle while reading.

function PhoneFlatIllustration({ width = 280, height = 200 }) {
  return (
    <div style={{ position: 'relative', width, height }}>
      <style>{`
        /* Phone tips DOWN from upright on the left, around the bottom-left
           corner where it meets the table surface. */
        @keyframes pfl-rock {
          0%, 14%   { transform: rotate(-90deg); }
          34%, 82%  { transform: rotate(0deg); }
          100%      { transform: rotate(-90deg); }
        }
        @keyframes pfl-fade-when-flat {
          0%, 28%   { opacity: 0; }
          44%, 82%  { opacity: 1; }
          100%      { opacity: 0; }
        }
        @keyframes pfl-fade-when-upright {
          0%, 18%   { opacity: 1; }
          30%       { opacity: 0; }
          82%       { opacity: 0; }
          100%      { opacity: 1; }
        }
        @keyframes pfl-shadow {
          0%, 28%   { transform: scaleX(0.15); opacity: 0; }
          44%, 82%  { transform: scaleX(1);    opacity: 1; }
          100%      { transform: scaleX(0.15); opacity: 0; }
        }
        .pfl-spin    { animation: pfl-rock              3.6s cubic-bezier(.55,.05,.25,1) infinite; transform-box: fill-box; }
        .pfl-flat    { animation: pfl-fade-when-flat    3.6s ease-in-out infinite; }
        .pfl-up      { animation: pfl-fade-when-upright 3.6s ease-in-out infinite; }
        .pfl-shadow  { animation: pfl-shadow            3.6s ease-in-out infinite; transform-origin: 50% 50%; transform-box: fill-box; }
      `}</style>

      <svg width={width} height={height} viewBox="0 0 280 200">
        <defs>
          <linearGradient id="pfl-surface" x1="0" x2="0" y1="0" y2="1">
            <stop offset="0%" stopColor="rgba(255,255,255,0.06)"/>
            <stop offset="100%" stopColor="rgba(255,255,255,0)"/>
          </linearGradient>
          <linearGradient id="pfl-phone" x1="0" x2="0" y1="0" y2="1">
            <stop offset="0%" stopColor="#2A2C46"/>
            <stop offset="100%" stopColor="#1A1C34"/>
          </linearGradient>
          <radialGradient id="pfl-glow" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stopColor="rgba(34,197,94,0.42)"/>
            <stop offset="100%" stopColor="rgba(34,197,94,0)"/>
          </radialGradient>
        </defs>

        {/* Table — perspective parallelogram */}
        <g>
          <path d="M40 168 L 246 168 L 230 188 L 56 188 Z"
                fill="url(#pfl-surface)" stroke="rgba(255,255,255,0.18)" strokeWidth="1.2"/>
          <text x="143" y="183" textAnchor="middle" fill="rgba(255,255,255,0.36)" fontSize="9" fontWeight="700" letterSpacing="1.6">TABLE · LEVEL</text>
        </g>

        {/* Green wash that appears when phone is flat */}
        <circle cx="160" cy="145" r="70" fill="url(#pfl-glow)" className="pfl-flat"/>

        {/* Shadow under phone when flat */}
        <ellipse cx="160" cy="170" rx="62" ry="3.2" fill="rgba(0,0,0,0.55)" className="pfl-shadow"/>

        {/* Downward arrow showing the motion — fades when phone is flat */}
        <g className="pfl-up" transform="translate(120 50)">
          <path d="M0 0 Q 20 30 50 50" fill="none" stroke="#22c55e" strokeWidth="2" strokeDasharray="3 4" strokeLinecap="round"/>
          <path d="M44 42 L 52 52 L 40 52" fill="none" stroke="#22c55e" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
        </g>

        {/* Mini compass disk floating above — fades in when phone is flat */}
        <g className="pfl-flat" transform="translate(160 56)">
          <circle r="22" fill="rgba(20,21,42,0.92)" stroke="rgba(34,197,94,0.45)" strokeWidth="1"/>
          <text y="-9" textAnchor="middle" fill="#EF4444" fontSize="9" fontWeight="800">N</text>
          <text y="15" textAnchor="middle" fill="rgba(255,255,255,0.6)" fontSize="7" fontWeight="700">S</text>
          <text x="-15" y="3" textAnchor="middle" fill="rgba(255,255,255,0.6)" fontSize="7" fontWeight="700">W</text>
          <text x="15"  y="3" textAnchor="middle" fill="rgba(255,255,255,0.6)" fontSize="7" fontWeight="700">E</text>
          <line x1="0" y1="0" x2="-11" y2="-11" stroke="#22c55e" strokeWidth="2.2" strokeLinecap="round"/>
          <circle r="2" fill="#fff"/>
        </g>

        {/* The phone — pivots at its bottom-left corner (94,152) so it tips
            down RIGHTWARD from upright into flat. */}
        <g className="pfl-spin" style={{ transformOrigin: '94px 152px' }}>
          {/* Phone body */}
          <rect x="94" y="138" width="132" height="14" rx="3" fill="url(#pfl-phone)"
                stroke="rgba(255,255,255,0.5)" strokeWidth="1.4"/>
          {/* Camera bump (on top edge when flat, on right edge when upright) */}
          <rect x="106" y="134" width="18" height="6" rx="1.5" fill="#2A2C46"
                stroke="rgba(255,255,255,0.4)" strokeWidth="1"/>
          <circle cx="115" cy="137" r="1.6" fill="rgba(34,197,94,0.85)"/>
          {/* Side button on the right (becomes top when upright) */}
          <rect x="224" y="142" width="3.5" height="4.5" rx="0.5" fill="rgba(255,255,255,0.35)"/>

          {/* Bubble level inset — only meaningful when flat, but lives on the phone */}
          <g transform="translate(140 144)">
            <rect x="0" y="0" width="56" height="6" rx="3"
                  fill="#0E0F22" stroke="rgba(255,255,255,0.25)" strokeWidth="0.8"/>
            <line x1="28" y1="-1.5" x2="28" y2="7.5" stroke="rgba(255,255,255,0.5)" strokeWidth="0.8"/>
            {/* Bubble — centered when flat (green), drifts when tilted (becomes invisible because phone is rotated). */}
            <circle cx="28" cy="3" r="2.4" fill="#22c55e"/>
          </g>
        </g>
      </svg>
    </div>
  );
}

function PhoneUprightIllustration({ width = 280, height = 200 }) {
  return (
    <div style={{ position: 'relative', width, height }}>
      <style>{`
        /* Phone starts horizontal in hand (screen up, like compass pose),
           rotates UP to vertical facing the wall. */
        @keyframes pul-rock {
          0%, 14%   { transform: rotate(90deg); }
          34%, 82%  { transform: rotate(0deg); }
          100%      { transform: rotate(90deg); }
        }
        @keyframes pul-fade-when-upright {
          0%, 28%   { opacity: 0; }
          44%, 82%  { opacity: 1; }
          100%      { opacity: 0; }
        }
        @keyframes pul-ray {
          0%, 28%   { stroke-dashoffset: 90; opacity: 0; }
          40%       { opacity: 1; }
          50%, 82%  { stroke-dashoffset: 0;  opacity: 1; }
          100%      { stroke-dashoffset: 90; opacity: 0; }
        }
        @keyframes pul-kaaba {
          0%, 32%   { opacity: 0.22; transform: scale(0.85); }
          50%, 82%  { opacity: 1;    transform: scale(1); }
          100%      { opacity: 0.22; transform: scale(0.85); }
        }
        @keyframes pul-fade-when-flat {
          0%, 18%   { opacity: 1; }
          30%       { opacity: 0; }
          82%       { opacity: 0; }
          100%      { opacity: 1; }
        }
        .pul-spin   { animation: pul-rock              3.6s cubic-bezier(.55,.05,.25,1) infinite; transform-box: fill-box; }
        .pul-up     { animation: pul-fade-when-upright 3.6s ease-in-out infinite; }
        .pul-flat   { animation: pul-fade-when-flat    3.6s ease-in-out infinite; }
        .pul-ray    { animation: pul-ray               3.6s ease-in-out infinite; }
        .pul-kaaba  { animation: pul-kaaba             3.6s ease-in-out infinite; transform-box: fill-box; transform-origin: center; }
      `}</style>

      <svg width={width} height={height} viewBox="0 0 280 200">
        <defs>
          <linearGradient id="pul-wall" x1="0" x2="0" y1="0" y2="1">
            <stop offset="0%" stopColor="rgba(255,255,255,0.10)"/>
            <stop offset="100%" stopColor="rgba(255,255,255,0.03)"/>
          </linearGradient>
          <linearGradient id="pul-phone-bg" x1="0" x2="0" y1="0" y2="1">
            <stop offset="0%" stopColor="#2A2C46"/>
            <stop offset="100%" stopColor="#1A1C34"/>
          </linearGradient>
          <radialGradient id="pul-kaaba-glow" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stopColor="rgba(34,197,94,0.42)"/>
            <stop offset="100%" stopColor="rgba(34,197,94,0)"/>
          </radialGradient>
        </defs>

        {/* Floor line for visual grounding */}
        <line x1="20" y1="178" x2="260" y2="178" stroke="rgba(255,255,255,0.12)" strokeWidth="1"/>
        <text x="40" y="194" fill="rgba(255,255,255,0.36)" fontSize="9" fontWeight="700" letterSpacing="1.6">YOU</text>
        <text x="240" y="194" textAnchor="middle" fill="rgba(255,255,255,0.36)" fontSize="9" fontWeight="700" letterSpacing="1.6">WALL</text>

        {/* Wall — vertical strip on the right with a faint frame for depth */}
        <g>
          <rect x="218" y="20" width="44" height="158" fill="url(#pul-wall)" stroke="rgba(255,255,255,0.18)" strokeWidth="1.2"/>
          <rect x="226" y="44" width="28" height="48" fill="none" stroke="rgba(255,255,255,0.18)" strokeWidth="1"/>
        </g>

        {/* Kaaba marker on the wall — emphasized when phone is upright */}
        <g transform="translate(240 110)" className="pul-kaaba">
          <circle r="22" fill="url(#pul-kaaba-glow)"/>
          <g transform="translate(-9 -9)" fill="none" stroke="#22c55e" strokeWidth="1.8" strokeLinejoin="round">
            <rect x="0.5" y="3" width="17" height="13" rx="0.4" fill="rgba(34,197,94,0.18)"/>
            <path d="M0.5 7H17.5M6 3v13M12 3v13"/>
          </g>
        </g>

        {/* Camera ray — appears when phone is upright, originating at upright
            phone's camera position (around 142,110) and ending at Kaaba marker. */}
        <line x1="146" y1="110" x2="234" y2="110"
              stroke="#22c55e" strokeWidth="2" strokeLinecap="round"
              strokeDasharray="6 8" className="pul-ray"
              style={{ strokeDasharray: '6 8' }}/>

        {/* Upward curved arrow — shows during the "lift up" motion, fades when upright */}
        <g className="pul-flat" transform="translate(180 90)">
          <path d="M0 60 Q -16 30 0 0" fill="none" stroke="#22c55e" strokeWidth="2" strokeDasharray="3 4" strokeLinecap="round"/>
          <path d="M-6 7 L 0 0 L 6 7" fill="none" stroke="#22c55e" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
        </g>

        {/* Hand grip — drawn OUTSIDE the rotating group so it stays at hand
            position while the phone rotates "in the hand". */}
        <g transform="translate(0 0)" opacity="0.85">
          <path d="M88 168 Q 92 184 120 184 L 124 184 Q 152 184 156 168 L 156 178 Q 152 192 124 192 L 120 192 Q 92 192 88 178 Z"
                fill="rgba(255,255,255,0.05)" stroke="rgba(255,255,255,0.2)" strokeWidth="1"/>
        </g>

        {/* Phone — pivots at hand position (120, 165). Starts horizontal in
            hand (screen up), rotates up to vertical facing wall. */}
        <g className="pul-spin" style={{ transformOrigin: '120px 165px' }}>
          {/* Phone body — drawn in its "vertical/upright" final orientation */}
          <rect x="102" y="78" width="36" height="88" rx="6"
                fill="url(#pul-phone-bg)" stroke="rgba(255,255,255,0.5)" strokeWidth="1.4"/>
          {/* Screen on left (user-facing side) */}
          <rect x="106" y="84" width="6" height="76" rx="1.5"
                fill="#0E0F22" stroke="rgba(255,255,255,0.18)" strokeWidth="0.5"/>
          {/* Notch on screen */}
          <circle cx="109" cy="88" r="1.2" fill="rgba(255,255,255,0.4)"/>
          {/* Camera lens on right (back, wall-facing side) */}
          <g transform="translate(130 90)">
            <rect x="0" y="0" width="10" height="14" rx="2" fill="#0A0A14"
                  stroke="rgba(255,255,255,0.4)" strokeWidth="0.8"/>
            <circle cx="5" cy="5" r="2.2" fill="none" stroke="rgba(34,197,94,0.9)" strokeWidth="1.2"/>
            <circle cx="5" cy="10" r="1.4" fill="none" stroke="rgba(255,255,255,0.45)" strokeWidth="0.8"/>
          </g>
        </g>
      </svg>
    </div>
  );
}

Object.assign(window, { PhoneFlatIllustration, PhoneUprightIllustration });
