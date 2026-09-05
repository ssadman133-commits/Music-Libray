// PulsePlay Interactive Web Preview Application
// Real Web Audio API synthesizer + VLC Gesture Video Simulation

// --- STATE MANAGEMENT ---
const state = {
  activeTab: 'music', // 'music' | 'videos'
  currentFilter: 'all',
  searchQuery: '',
  isPlaying: false,
  currentSongIndex: 0,
  currentTime: 0,
  duration: 230,
  volume: 0.85,
  isMuted: false,
  isShuffle: false,
  repeatMode: 0, // 0: off, 1: all, 2: one
  isFullPlayerOpen: false,
  isVideoPlaying: false,
  currentVideoIndex: 0,
  videoTime: 0,
  videoDuration: 312,
  videoBrightness: 80,
  videoVolume: 85,
  isVideoLocked: false,
  showVideoControls: true,
  mediaToDelete: null
};

// --- DATA CATALOG ---
const songs = [
  {
    id: 1,
    title: "Starboy (feat. Daft Punk)",
    artist: "The Weeknd",
    album: "Starboy",
    duration: 230,
    isFavorite: true,
    artColor: "linear-gradient(135deg, #FF007A, #7928CA)",
    icon: "⚡",
    bpm: 120,
    baseFreq: 130.81 // C3
  },
  {
    id: 2,
    title: "Blinding Lights",
    artist: "The Weeknd",
    album: "After Hours",
    duration: 200,
    isFavorite: false,
    artColor: "linear-gradient(135deg, #FF4B2B, #FF416C)",
    icon: "🕶️",
    bpm: 171,
    baseFreq: 146.83 // D3
  },
  {
    id: 3,
    title: "Midnight City",
    artist: "M83",
    album: "Hurry Up, We're Dreaming",
    duration: 243,
    isFavorite: true,
    artColor: "linear-gradient(135deg, #00B4D8, #9D4EDD)",
    icon: "🌃",
    bpm: 105,
    baseFreq: 164.81 // E3
  },
  {
    id: 4,
    title: "Get Lucky (feat. Pharrell)",
    artist: "Daft Punk",
    album: "Random Access Memories",
    duration: 248,
    isFavorite: false,
    artColor: "linear-gradient(135deg, #F59E0B, #D97706)",
    icon: "✨",
    bpm: 116,
    baseFreq: 116.54 // Bb2
  },
  {
    id: 5,
    title: "Resonance",
    artist: "HOME",
    album: "Odyssey",
    duration: 212,
    isFavorite: true,
    artColor: "linear-gradient(135deg, #06B6D4, #3B82F6)",
    icon: "🌌",
    bpm: 85,
    baseFreq: 174.61 // F3
  }
];

const videos = [
  {
    id: 10,
    title: "Interstellar - Docking Scene (4K IMAX)",
    resolution: "3840x2160",
    duration: 312,
    size: "238.4 MB",
    folder: "Movies",
    type: "space"
  },
  {
    id: 11,
    title: "Coldplay - Fix You (Live at São Paulo)",
    resolution: "1920x1080",
    duration: 285,
    size: "171.7 MB",
    folder: "Concerts",
    type: "concert"
  }
];

// --- WEB AUDIO API SYNTHESIZER & VISUALIZER ---
let audioCtx = null;
let analyser = null;
let synthInterval = null;
let masterGain = null;

function initAudio() {
  if (audioCtx) return;
  try {
    const AudioContext = window.AudioContext || window.webkitAudioContext;
    audioCtx = new AudioContext();
    analyser = audioCtx.createAnalyser();
    analyser.fftSize = 64;
    masterGain = audioCtx.createGain();
    masterGain.gain.value = state.volume;
    masterGain.connect(analyser);
    analyser.connect(audioCtx.destination);
  } catch (e) {
    console.warn("AudioContext not supported or blocked", e);
  }
}

// Procedural Beat Generator (so real music plays in headphones/speakers)
function playTone(freq, type, duration, gainVal = 0.15) {
  if (!audioCtx) return;
  try {
    const osc = audioCtx.createOscillator();
    const gain = audioCtx.createGain();
    osc.type = type;
    osc.frequency.setValueAtTime(freq, audioCtx.currentTime);
    
    gain.gain.setValueAtTime(gainVal * (state.isMuted ? 0 : state.volume), audioCtx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.0001, audioCtx.currentTime + duration);
    
    osc.connect(gain);
    gain.connect(masterGain);
    
    osc.start();
    osc.stop(audioCtx.currentTime + duration);
  } catch(e) {}
}

let step = 0;
function startBeatGenerator() {
  stopBeatGenerator();
  const current = songs[state.currentSongIndex];
  const stepTime = (60 / current.bpm) * 500; // 8th note interval

  synthInterval = setInterval(() => {
    if (!state.isPlaying) return;
    
    // Kick on 1 and 3
    if (step % 4 === 0) {
      playTone(60, 'sine', 0.25, 0.35);
    }
    // Snare / Hi-Hat on offbeats
    if (step % 2 === 1) {
      playTone(1800, 'triangle', 0.08, 0.08);
    }
    // Bass note
    const bassMultiplier = [1, 1.25, 1.5, 1.33][Math.floor(step / 4) % 4];
    playTone(current.baseFreq * bassMultiplier, 'sawtooth', 0.3, 0.12);

    // Arpeggio melody note
    const arpFreq = current.baseFreq * 2 * [1, 1.2, 1.5, 1.875, 2][step % 5];
    playTone(arpFreq, 'sine', 0.18, 0.1);

    step++;
  }, stepTime);
}

function stopBeatGenerator() {
  if (synthInterval) {
    clearInterval(synthInterval);
    synthInterval = null;
  }
}

// Visualizer Canvas Loop
function setupVisualizer() {
  const canvas = document.getElementById('visualizerCanvas');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');

  function renderVisualizer() {
    requestAnimationFrame(renderVisualizer);
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    const barCount = 32;
    const barWidth = (canvas.width / barCount) - 3;

    let dataArray = new Uint8Array(barCount);
    if (analyser && state.isPlaying) {
      analyser.getByteFrequencyData(dataArray);
    }

    for (let i = 0; i < barCount; i++) {
      let val = dataArray[i];
      if (!state.isPlaying || !analyser) {
        val = 8 + Math.sin(Date.now() * 0.005 + i * 0.3) * 6;
      }
      const barHeight = (val / 255) * (canvas.height - 4);
      const x = i * (barWidth + 3);
      const y = canvas.height - barHeight;

      const gradient = ctx.createLinearGradient(0, canvas.height, 0, 0);
      gradient.addColorStop(0, '#8B5CF6');
      gradient.addColorStop(1, '#FF4D4D');

      ctx.fillStyle = gradient;
      ctx.beginPath();
      ctx.roundRect(x, y, barWidth, barHeight, [3, 3, 0, 0]);
      ctx.fill();
    }
  }
  renderVisualizer();
}

// --- DOM ELEMENTS ---
const elements = {
  clock: document.getElementById('statusClock'),
  splash: document.getElementById('splashScreen'),
  permissions: document.getElementById('permissionScreen'),
  trackList: document.getElementById('trackList'),
  videoList: document.getElementById('videoList'),
  trackCountLabel: document.getElementById('trackCountLabel'),
  searchInput: document.getElementById('searchInput'),
  btnClearSearch: document.getElementById('btnClearSearch'),
  filterChips: document.getElementById('filterChips'),
  musicView: document.getElementById('musicView'),
  videoView: document.getElementById('videoView'),
  navMusicBtn: document.getElementById('navMusicBtn'),
  navVideosBtn: document.getElementById('navVideosBtn'),
  // Mini Player
  miniPlayer: document.getElementById('miniPlayer'),
  miniTitle: document.getElementById('miniTitle'),
  miniArtist: document.getElementById('miniArtist'),
  miniArt: document.getElementById('miniArt'),
  miniPlayBtn: document.getElementById('miniPlayBtn'),
  miniPlayIcon: document.getElementById('miniPlayIcon'),
  miniPrevBtn: document.getElementById('miniPrevBtn'),
  miniNextBtn: document.getElementById('miniNextBtn'),
  miniProgressFill: document.getElementById('miniProgressFill'),
  // Full Player Sheet
  fullPlayerSheet: document.getElementById('fullPlayerSheet'),
  fullPlayerTitle: document.getElementById('fullPlayerTitle'),
  fullPlayerArtist: document.getElementById('fullPlayerArtist'),
  fullPlayerAlbum: document.getElementById('fullPlayerAlbum'),
  vinylDisc: document.getElementById('vinylDisc'),
  vinylCenter: document.getElementById('vinylCenter'),
  scrubberTrack: document.getElementById('scrubberTrack'),
  scrubberFill: document.getElementById('scrubberFill'),
  scrubberThumb: document.getElementById('scrubberThumb'),
  timeElapsed: document.getElementById('timeElapsed'),
  timeDuration: document.getElementById('timeDuration'),
  btnMainPlay: document.getElementById('btnMainPlay'),
  mainPlayIcon: document.getElementById('mainPlayIcon'),
  btnPrevTrack: document.getElementById('btnPrevTrack'),
  btnNextTrack: document.getElementById('btnNextTrack'),
  btnShuffle: document.getElementById('btnShuffle'),
  btnRepeat: document.getElementById('btnRepeat'),
  btnFullFavorite: document.getElementById('btnFullFavorite'),
  fullFavIcon: document.getElementById('fullFavIcon'),
  volumeTrack: document.getElementById('volumeTrack'),
  volumeFill: document.getElementById('volumeFill'),
  btnMute: document.getElementById('btnMute'),
  // VLC Screen
  vlcPlayerScreen: document.getElementById('vlcPlayerScreen'),
  vlcCanvas: document.getElementById('vlcCanvas'),
  vlcOverlay: document.getElementById('vlcOverlay'),
  vlcTitle: document.getElementById('vlcTitle'),
  btnVlcBack: document.getElementById('btnVlcBack'),
  btnVlcPlay: document.getElementById('btnVlcPlay'),
  vlcPlayIcon: document.getElementById('vlcPlayIcon'),
  btnVlcLock: document.getElementById('btnVlcLock'),
  vlcScrubberTrack: document.getElementById('vlcScrubberTrack'),
  vlcScrubberFill: document.getElementById('vlcScrubberFill'),
  vlcTimeElapsed: document.getElementById('vlcTimeElapsed'),
  vlcTimeTotal: document.getElementById('vlcTimeTotal'),
  vlcBrightnessHud: document.getElementById('vlcBrightnessHud'),
  vlcBrightnessFill: document.getElementById('vlcBrightnessFill'),
  vlcBrightnessLabel: document.getElementById('vlcBrightnessLabel'),
  vlcVolumeHud: document.getElementById('vlcVolumeHud'),
  vlcVolumeFill: document.getElementById('vlcVolumeFill'),
  vlcVolumeLabel: document.getElementById('vlcVolumeLabel'),
  vlcRewindRipple: document.getElementById('vlcRewindRipple'),
  vlcForwardRipple: document.getElementById('vlcForwardRipple'),
  // Modals & Frame
  deviceFrame: document.getElementById('deviceFrame'),
  screenViewport: document.getElementById('screenViewport'),
  btnDeviceFrame: document.getElementById('btnDeviceFrame'),
  btnRestartSplash: document.getElementById('btnRestartSplash'),
  btnTogglePermissions: document.getElementById('btnTogglePermissions'),
  btnAddSample: document.getElementById('btnAddSample'),
  deleteModal: document.getElementById('deleteModal'),
  deleteModalDesc: document.getElementById('deleteModalDesc'),
  btnCancelDelete: document.getElementById('btnCancelDelete'),
  btnConfirmDelete: document.getElementById('btnConfirmDelete'),
  toastContainer: document.getElementById('toastContainer')
};

// --- TIME FORMAT HELPER ---
function formatTime(seconds) {
  const mins = Math.floor(seconds / 60);
  const secs = Math.floor(seconds % 60);
  return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
}

// --- RENDER FUNCTIONS ---
function renderSongs() {
  const query = state.searchQuery.toLowerCase();
  const filtered = songs.filter(song => {
    const matchesSearch = song.title.toLowerCase().includes(query) ||
                          song.artist.toLowerCase().includes(query) ||
                          song.album.toLowerCase().includes(query);
    if (!matchesSearch) return false;
    if (state.currentFilter === 'favorites') return song.isFavorite;
    return true;
  });

  elements.trackCountLabel.textContent = `${filtered.length} Tracks Available`;

  if (filtered.length === 0) {
    elements.trackList.innerHTML = `
      <div style="text-align: center; padding: 48px 16px; color: var(--text-secondary);">
        <div style="font-size: 36px; margin-bottom: 8px;">🔍</div>
        <p style="font-weight: 600;">No matching tracks found</p>
        <span style="font-size: 12px;">Try adjusting your search or filters</span>
      </div>`;
    return;
  }

  elements.trackList.innerHTML = filtered.map((song, index) => {
    const isCurrent = (songs[state.currentSongIndex].id === song.id);
    const isPlayingCurrent = isCurrent && state.isPlaying;

    return `
      <div class="song-card ${isCurrent ? 'playing' : ''}" onclick="selectAndPlaySong(${song.id})">
        <div class="song-art" style="background: ${song.artColor};">
          <span>${song.icon}</span>
        </div>
        <div class="song-info">
          <div class="song-title">
            ${isPlayingCurrent ? `
              <div class="playing-bars">
                <span></span><span></span><span></span>
              </div>` : ''}
            <span>${song.title}</span>
          </div>
          <div class="song-meta">${song.artist} • ${song.album}</div>
        </div>
        <div class="song-actions" onclick="event.stopPropagation()">
          <span class="song-duration">${formatTime(song.duration)}</span>
          <button class="fav-btn ${song.isFavorite ? 'active' : ''}" onclick="toggleFavorite(${song.id})" title="Favorite">
            ${song.isFavorite ? '❤️' : '🤍'}
          </button>
          <button class="more-btn" onclick="promptDelete(${song.id}, 'song')" title="Delete">
            🗑️
          </button>
        </div>
      </div>
    `;
  }).join('');
}

function renderVideos() {
  elements.videoList.innerHTML = videos.map(video => `
    <div class="video-card" onclick="openVlcPlayer(${video.id})">
      <div class="video-thumb">
        <div class="video-play-indicator">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
        </div>
        <span class="badge-res">${video.resolution}</span>
        <span class="badge-duration">${formatTime(video.duration)}</span>
      </div>
      <div class="video-info-row">
        <div>
          <div class="video-title">${video.title}</div>
          <div class="video-sub">${video.folder} • ${video.size}</div>
        </div>
        <button class="more-btn" onclick="event.stopPropagation(); promptDelete(${video.id}, 'video')">
          🗑️
        </button>
      </div>
    </div>
  `).join('');
}

function updatePlaybackUI() {
  const current = songs[state.currentSongIndex];
  if (!current) return;

  state.duration = current.duration;

  // Mini Player
  elements.miniTitle.textContent = current.title;
  elements.miniArtist.textContent = current.artist;
  elements.miniArt.innerHTML = current.icon;
  elements.miniArt.style.background = current.artColor;

  const playSvg = `<path d="M8 5v14l11-7z"/>`;
  const pauseSvg = `<path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/>`;

  elements.miniPlayIcon.innerHTML = state.isPlaying ? pauseSvg : playSvg;
  elements.mainPlayIcon.innerHTML = state.isPlaying ? pauseSvg : playSvg;

  // Progress calculations
  const progressPercent = (state.currentTime / state.duration) * 100;
  elements.miniProgressFill.style.width = `${progressPercent}%`;
  elements.scrubberFill.style.width = `${progressPercent}%`;
  elements.scrubberThumb.style.left = `${progressPercent}%`;

  elements.timeElapsed.textContent = formatTime(state.currentTime);
  elements.timeDuration.textContent = formatTime(state.duration);

  // Full Player
  elements.fullPlayerTitle.textContent = current.title;
  elements.fullPlayerArtist.textContent = current.artist;
  elements.fullPlayerAlbum.textContent = current.album;
  elements.vinylCenter.style.background = current.artColor;
  elements.vinylCenter.innerHTML = `<span style="font-size: 36px">${current.icon}</span>`;

  if (state.isPlaying) {
    elements.vinylDisc.classList.add('playing');
  } else {
    elements.vinylDisc.classList.remove('playing');
  }

  // Favorite button
  elements.fullFavIcon.setAttribute('fill', current.isFavorite ? 'var(--accent-red)' : 'none');
  elements.fullFavIcon.style.color = current.isFavorite ? 'var(--accent-red)' : '#fff';

  renderSongs();
}

// --- AUDIO PLAYBACK ENGINE ---
let timerInterval = null;

function selectAndPlaySong(id) {
  initAudio();
  const index = songs.findIndex(s => s.id === id);
  if (index !== -1) {
    state.currentSongIndex = index;
    state.currentTime = 0;
    playAudio();
  }
}

function playAudio() {
  initAudio();
  if (audioCtx && audioCtx.state === 'suspended') {
    audioCtx.resume();
  }
  state.isPlaying = true;
  updatePlaybackUI();
  startBeatGenerator();

  clearInterval(timerInterval);
  timerInterval = setInterval(() => {
    if (state.isPlaying) {
      state.currentTime += 1;
      if (state.currentTime >= state.duration) {
        handleTrackEnd();
      }
      updatePlaybackUI();
    }
  }, 1000);
}

function pauseAudio() {
  state.isPlaying = false;
  stopBeatGenerator();
  updatePlaybackUI();
}

function togglePlayPause() {
  if (state.isPlaying) {
    pauseAudio();
  } else {
    playAudio();
  }
}

function handleTrackEnd() {
  if (state.repeatMode === 2) {
    // Repeat one
    state.currentTime = 0;
    playAudio();
  } else {
    nextTrack();
  }
}

function nextTrack() {
  if (state.isShuffle) {
    state.currentSongIndex = Math.floor(Math.random() * songs.length);
  } else {
    state.currentSongIndex = (state.currentSongIndex + 1) % songs.length;
  }
  state.currentTime = 0;
  playAudio();
  showToast(`Playing next: ${songs[state.currentSongIndex].title}`);
}

function prevTrack() {
  if (state.currentTime > 4) {
    state.currentTime = 0;
  } else {
    state.currentSongIndex = (state.currentSongIndex - 1 + songs.length) % songs.length;
    state.currentTime = 0;
  }
  playAudio();
  showToast(`Playing: ${songs[state.currentSongIndex].title}`);
}

function toggleFavorite(id) {
  const song = songs.find(s => s.id === id);
  if (song) {
    song.isFavorite = !song.isFavorite;
    updatePlaybackUI();
    showToast(song.isFavorite ? `Added "${song.title}" to Favorites ❤️` : `Removed from Favorites`);
  }
}

// Scrubber seeking
function seekToPercent(pct) {
  state.currentTime = Math.floor(state.duration * pct);
  updatePlaybackUI();
}

// Volume
function setVolume(pct) {
  state.volume = Math.max(0, Math.min(1, pct));
  if (masterGain) {
    masterGain.gain.setValueAtTime(state.volume, audioCtx.currentTime);
  }
  elements.volumeFill.style.width = `${state.volume * 100}%`;
}

// --- VLC VIDEO PLAYER SIMULATION & GESTURES ---
let vlcInterval = null;
let vlcAnimId = null;

function openVlcPlayer(videoId) {
  pauseAudio();
  const vIndex = videos.findIndex(v => v.id === videoId);
  state.currentVideoIndex = vIndex !== -1 ? vIndex : 0;
  const vid = videos[state.currentVideoIndex];

  state.videoTime = 0;
  state.videoDuration = vid.duration;
  state.isVideoPlaying = true;
  state.isVideoLocked = false;
  state.showVideoControls = true;

  elements.vlcTitle.textContent = vid.title;
  elements.vlcTimeTotal.textContent = formatTime(vid.duration);
  elements.vlcPlayerScreen.classList.add('active');

  startVlcAnimation(vid.type);
  startVlcPlayback();
  showToast("VLC Gestures active: Swipe Left/Right to adjust");
}

function closeVlcPlayer() {
  state.isVideoPlaying = false;
  elements.vlcPlayerScreen.classList.remove('active');
  clearInterval(vlcInterval);
  cancelAnimationFrame(vlcAnimId);
}

function startVlcPlayback() {
  clearInterval(vlcInterval);
  vlcInterval = setInterval(() => {
    if (state.isVideoPlaying) {
      state.videoTime++;
      if (state.videoTime >= state.videoDuration) {
        state.videoTime = 0;
      }
      updateVlcUI();
    }
  }, 1000);
}

function updateVlcUI() {
  const pct = (state.videoTime / state.videoDuration) * 100;
  elements.vlcScrubberFill.style.width = `${pct}%`;
  elements.vlcTimeElapsed.textContent = formatTime(state.videoTime);

  const playSvg = `<path d="M8 5v14l11-7z"/>`;
  const pauseSvg = `<path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/>`;
  elements.vlcPlayIcon.innerHTML = state.isVideoPlaying ? pauseSvg : playSvg;
}

// Canvas Visual Generator for simulated video footage
function startVlcAnimation(type) {
  const canvas = elements.vlcCanvas;
  const ctx = canvas.getContext('2d');
  let frame = 0;

  // Starfield particles
  const stars = Array.from({ length: 90 }, () => ({
    x: Math.random() * canvas.width,
    y: Math.random() * canvas.height,
    speed: 0.5 + Math.random() * 2,
    size: 1 + Math.random() * 2
  }));

  function renderVideo() {
    if (!state.isVideoPlaying && !elements.vlcPlayerScreen.classList.contains('active')) return;
    vlcAnimId = requestAnimationFrame(renderVideo);
    frame++;

    // Apply brightness filter from state
    ctx.filter = `brightness(${state.videoBrightness}%)`;

    // Background
    ctx.fillStyle = '#06050b';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    if (type === 'space') {
      // Procedural space warp / docking scene
      stars.forEach(s => {
        s.y += s.speed;
        if (s.y > canvas.height) s.y = 0;
        ctx.fillStyle = '#ffffff';
        ctx.beginPath();
        ctx.arc(s.x, s.y, s.size, 0, Math.PI * 2);
        ctx.fill();
      });

      // Rotating Station
      ctx.save();
      ctx.translate(canvas.width / 2, canvas.height / 2);
      ctx.rotate(frame * 0.015);
      
      const grad = ctx.createLinearGradient(-60, -60, 60, 60);
      grad.addColorStop(0, '#00B4D8');
      grad.addColorStop(1, '#9D4EDD');
      ctx.strokeStyle = grad;
      ctx.lineWidth = 4;
      ctx.beginPath();
      ctx.arc(0, 0, 70, 0, Math.PI * 2);
      ctx.stroke();

      ctx.fillStyle = '#FF007A';
      ctx.fillRect(-8, -8, 16, 16);
      ctx.restore();

    } else {
      // Concert laser / stage lighting
      for (let i = 0; i < 5; i++) {
        const x = (canvas.width / 4) * i;
        ctx.strokeStyle = `hsla(${(frame * 2 + i * 50) % 360}, 90%, 65%, 0.6)`;
        ctx.lineWidth = 6;
        ctx.beginPath();
        ctx.moveTo(canvas.width / 2, canvas.height);
        ctx.lineTo(x, 60 + Math.sin(frame * 0.05 + i) * 80);
        ctx.stroke();
      }
    }
  }
  renderVideo();
}

// VLC Gestures: Swipe Brightness & Volume + Double-Tap Seek
function setupVlcGestures() {
  const surface = document.getElementById('vlcSurface');
  const overlay = elements.vlcOverlay;
  let touchStartX = 0;
  let touchStartY = 0;
  let isDragging = false;
  let didDrag = false;
  let lastTapTime = 0;
  let hudHideTimeout = null;

  function onPointerDown(e) {
    if (state.isVideoLocked) return;
    // Only handle if clicking empty area, surface or overlay background (not buttons/sliders)
    if (e.target.closest('button') || e.target.closest('.vlc-scrubber-row')) return;
    touchStartX = e.clientX;
    touchStartY = e.clientY;
    isDragging = true;
    didDrag = false;
  }

  surface.addEventListener('pointerdown', onPointerDown);
  overlay.addEventListener('pointerdown', onPointerDown);

  window.addEventListener('pointermove', (e) => {
    if (!isDragging || state.isVideoLocked) return;
    const deltaY = touchStartY - e.clientY;
    const rect = surface.getBoundingClientRect();
    const isLeft = (touchStartX - rect.left) < (rect.width / 2);

    if (Math.abs(deltaY) > 4) {
      didDrag = true;
      clearTimeout(hudHideTimeout);

      if (isLeft) {
        // Brightness (0 - 100%)
        state.videoBrightness = Math.max(5, Math.min(100, (state.videoBrightness || 80) + (deltaY > 0 ? 1.5 : -1.5)));
        elements.vlcBrightnessHud.classList.remove('hidden');
        elements.vlcBrightnessFill.style.height = `${state.videoBrightness}%`;
        elements.vlcBrightnessLabel.textContent = `☀️ ${Math.round(state.videoBrightness)}%`;
      } else {
        // Volume (0 - 100%)
        state.videoVolume = Math.max(0, Math.min(100, (state.videoVolume || 85) + (deltaY > 0 ? 1.5 : -1.5)));
        elements.vlcVolumeHud.classList.remove('hidden');
        elements.vlcVolumeFill.style.height = `${state.videoVolume}%`;
        elements.vlcVolumeLabel.textContent = `🔊 ${Math.round(state.videoVolume)}%`;
      }
      touchStartY = e.clientY;
    }
  });

  window.addEventListener('pointerup', (e) => {
    if (isDragging) {
      isDragging = false;
      hudHideTimeout = setTimeout(() => {
        elements.vlcBrightnessHud.classList.add('hidden');
        elements.vlcVolumeHud.classList.add('hidden');
      }, 700);
    }
  });

  // Double tap to seek (+10s right / -10s left) or Single tap to toggle controls
  function handleVideoTap(e) {
    if (state.isVideoLocked || didDrag) return;
    if (e.target.closest('button') || e.target.closest('.vlc-scrubber-row')) return;

    const currentTime = Date.now();
    const rect = surface.getBoundingClientRect();
    const clickX = e.clientX - rect.left;

    if (currentTime - lastTapTime < 340) {
      // Double Tap detected on left half vs right half
      const isRight = clickX > rect.width / 2;
      if (isRight) {
        // Forward 10s
        state.videoTime = Math.min(state.videoDuration, state.videoTime + 10);
        triggerSeekRipple(elements.vlcForwardRipple);
      } else {
        // Rewind 10s
        state.videoTime = Math.max(0, state.videoTime - 10);
        triggerSeekRipple(elements.vlcRewindRipple);
      }
      updateVlcUI();
    } else {
      // Single Tap: Toggle overlay controls
      state.showVideoControls = !state.showVideoControls;
      elements.vlcOverlay.classList.toggle('controls-hidden', !state.showVideoControls);
    }
    lastTapTime = currentTime;
  }

  surface.addEventListener('click', handleVideoTap);
  overlay.addEventListener('click', handleVideoTap);
}

function triggerSeekRipple(el) {
  el.classList.remove('hidden');
  setTimeout(() => el.classList.add('hidden'), 550);
}

// --- DELETE MEDIA DIALOG ---
function promptDelete(id, type) {
  state.mediaToDelete = { id, type };
  const item = type === 'song' ? songs.find(s => s.id === id) : videos.find(v => v.id === id);
  if (!item) return;

  elements.deleteModalDesc.textContent = `Are you sure you want to delete "${item.title}"? This will remove it from your device storage.`;
  elements.deleteModal.classList.remove('hidden');
}

function confirmDelete() {
  if (!state.mediaToDelete) return;
  const { id, type } = state.mediaToDelete;

  if (type === 'song') {
    const idx = songs.findIndex(s => s.id === id);
    if (idx !== -1) {
      const removed = songs.splice(idx, 1)[0];
      if (state.currentSongIndex >= songs.length) state.currentSongIndex = 0;
      renderSongs();
      updatePlaybackUI();
      showToast(`Deleted track "${removed.title}"`);
    }
  } else {
    const idx = videos.findIndex(v => v.id === id);
    if (idx !== -1) {
      const removed = videos.splice(idx, 1)[0];
      renderVideos();
      showToast(`Deleted video "${removed.title}"`);
    }
  }

  elements.deleteModal.classList.add('hidden');
  state.mediaToDelete = null;
}

// --- TOAST NOTIFICATIONS ---
function showToast(msg) {
  const toast = document.createElement('div');
  toast.className = 'toast';
  toast.textContent = msg;
  elements.toastContainer.appendChild(toast);
  setTimeout(() => toast.remove(), 2500);
}

// --- CLOCK ---
function updateClock() {
  const now = new Date();
  const hours = now.getHours().toString().padStart(2, '0');
  const minutes = now.getMinutes().toString().padStart(2, '0');
  elements.clock.textContent = `${hours}:${minutes}`;
}

// --- EVENT LISTENERS INITIALIZATION ---
function initEvents() {
  // Splash Screen auto fadeout
  setTimeout(() => {
    elements.splash.classList.add('fade-out');
  }, 1400);

  // Top control bar actions
  if (elements.btnDeviceFrame) {
    elements.btnDeviceFrame.addEventListener('click', () => {
      elements.deviceFrame.classList.toggle('no-frame');
      elements.btnDeviceFrame.classList.toggle('active');
    });
  }

  if (elements.btnRestartSplash) {
    elements.btnRestartSplash.addEventListener('click', () => {
      elements.splash.classList.remove('fade-out');
      setTimeout(() => elements.splash.classList.add('fade-out'), 1400);
    });
  }

  if (elements.btnTogglePermissions) {
    elements.btnTogglePermissions.addEventListener('click', () => {
      elements.permissions.classList.toggle('hidden');
    });
  }

  const btnGrantPerm = document.getElementById('btnGrantPermission');
  if (btnGrantPerm) {
    btnGrantPerm.addEventListener('click', () => {
      elements.permissions.classList.add('hidden');
      showToast("Storage permissions granted! Real-time scanning active.");
    });
  }

  const btnClosePerm = document.getElementById('btnClosePermission');
  if (btnClosePerm) {
    btnClosePerm.addEventListener('click', () => {
      elements.permissions.classList.add('hidden');
    });
  }

  if (elements.btnAddSample) {
    elements.btnAddSample.addEventListener('click', () => {
      const newId = Date.now();
      songs.push({
        id: newId,
        title: "Neon Horizon (Synthwave)",
        artist: "PulsePlay Original",
        album: "Cyber Sounds",
        duration: 198,
        isFavorite: false,
        artColor: "linear-gradient(135deg, #EC4899, #8B5CF6)",
        icon: "🎸",
        bpm: 125,
        baseFreq: 138.59
      });
      renderSongs();
      showToast("Added new track to library!");
    });
  }

  // Search
  elements.searchInput.addEventListener('input', (e) => {
    state.searchQuery = e.target.value;
    elements.btnClearSearch.classList.toggle('hidden', state.searchQuery.length === 0);
    renderSongs();
  });

  elements.btnClearSearch.addEventListener('click', () => {
    elements.searchInput.value = '';
    state.searchQuery = '';
    elements.btnClearSearch.classList.add('hidden');
    renderSongs();
  });

  // Filter Chips
  elements.filterChips.querySelectorAll('.chip').forEach(chip => {
    chip.addEventListener('click', () => {
      elements.filterChips.querySelectorAll('.chip').forEach(c => c.classList.remove('active'));
      chip.classList.add('active');
      state.currentFilter = chip.dataset.filter;
      renderSongs();
    });
  });

  // Bottom Navigation
  elements.navMusicBtn.addEventListener('click', () => {
    elements.navMusicBtn.classList.add('active');
    elements.navVideosBtn.classList.remove('active');
    elements.musicView.classList.remove('hidden');
    elements.videoView.classList.add('hidden');
    elements.filterChips.classList.remove('hidden');
  });

  elements.navVideosBtn.addEventListener('click', () => {
    elements.navVideosBtn.classList.add('active');
    elements.navMusicBtn.classList.remove('active');
    elements.videoView.classList.remove('hidden');
    elements.musicView.classList.add('hidden');
    elements.filterChips.classList.add('hidden');
    renderVideos();
  });

  // Mini-Player click -> Open Full Player
  document.getElementById('miniPlayerClickable').addEventListener('click', () => {
    elements.fullPlayerSheet.classList.add('open');
    state.isFullPlayerOpen = true;
  });

  elements.miniPlayBtn.addEventListener('click', togglePlayPause);
  elements.miniNextBtn.addEventListener('click', nextTrack);
  elements.miniPrevBtn.addEventListener('click', prevTrack);

  // Full Player Controls
  document.getElementById('btnCollapseSheet').addEventListener('click', () => {
    elements.fullPlayerSheet.classList.remove('open');
    state.isFullPlayerOpen = false;
  });

  document.getElementById('sheetDragHandle').addEventListener('click', () => {
    elements.fullPlayerSheet.classList.remove('open');
    state.isFullPlayerOpen = false;
  });

  elements.btnMainPlay.addEventListener('click', togglePlayPause);
  elements.btnNextTrack.addEventListener('click', nextTrack);
  elements.btnPrevTrack.addEventListener('click', prevTrack);

  elements.btnShuffle.addEventListener('click', () => {
    state.isShuffle = !state.isShuffle;
    elements.btnShuffle.classList.toggle('active', state.isShuffle);
    showToast(state.isShuffle ? "Shuffle Mode ON" : "Shuffle Mode OFF");
  });

  elements.btnRepeat.addEventListener('click', () => {
    state.repeatMode = (state.repeatMode + 1) % 3;
    const modes = ["Repeat OFF", "Repeat ALL 🔁", "Repeat ONE 🔂"];
    elements.btnRepeat.classList.toggle('active', state.repeatMode > 0);
    showToast(modes[state.repeatMode]);
  });

  elements.btnFullFavorite.addEventListener('click', () => {
    toggleFavorite(songs[state.currentSongIndex].id);
  });

  // Scrubber click & drag
  function handleScrubber(e) {
    const rect = elements.scrubberTrack.getBoundingClientRect();
    const pct = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
    seekToPercent(pct);
  }
  elements.scrubberTrack.addEventListener('click', handleScrubber);

  // Volume click
  elements.volumeTrack.addEventListener('click', (e) => {
    const rect = elements.volumeTrack.getBoundingClientRect();
    const pct = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
    setVolume(pct);
  });

  elements.btnMute.addEventListener('click', () => {
    state.isMuted = !state.isMuted;
    if (masterGain) {
      masterGain.gain.setValueAtTime(state.isMuted ? 0 : state.volume, audioCtx.currentTime);
    }
    showToast(state.isMuted ? "Audio Muted 🔇" : "Audio Unmuted 🔊");
  });

  // VLC Player Controls
  elements.btnVlcBack.addEventListener('click', closeVlcPlayer);
  elements.btnVlcPlay.addEventListener('click', () => {
    state.isVideoPlaying = !state.isVideoPlaying;
    updateVlcUI();
  });

  elements.btnVlcLock.addEventListener('click', () => {
    state.isVideoLocked = !state.isVideoLocked;
    elements.btnVlcLock.classList.toggle('active', state.isVideoLocked);
    showToast(state.isVideoLocked ? "Screen Controls Locked 🔒" : "Screen Controls Unlocked 🔓");
  });

  elements.vlcScrubberTrack.addEventListener('click', (e) => {
    const rect = elements.vlcScrubberTrack.getBoundingClientRect();
    const pct = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
    state.videoTime = Math.floor(state.videoDuration * pct);
    updateVlcUI();
  });

  const btnVlcRotate = document.getElementById('btnVlcRotate');
  if (btnVlcRotate) {
    btnVlcRotate.addEventListener('click', (e) => {
      e.stopPropagation();
      elements.deviceFrame.classList.toggle('landscape');
      const isLand = elements.deviceFrame.classList.contains('landscape');
      showToast(isLand ? "Rotated to Landscape 🔄" : "Rotated to Portrait 📱");
    });
  }

  // Sync / Refresh
  document.getElementById('btnSyncLibrary').addEventListener('click', () => {
    showToast("🔄 Scanned storage: Library is up to date!");
  });

  // Delete Modal
  elements.btnCancelDelete.addEventListener('click', () => {
    elements.deleteModal.classList.add('hidden');
    state.mediaToDelete = null;
  });

  elements.btnConfirmDelete.addEventListener('click', confirmDelete);

  // Android Hardware Navigation Toolbar Listeners (AI Studio style)
  const hwBackBtn = document.getElementById('hwBackBtn');
  if (hwBackBtn) {
    hwBackBtn.addEventListener('click', () => {
      if (elements.vlcPlayerScreen.classList.contains('active')) {
        closeVlcPlayer();
      } else if (state.isFullPlayerOpen) {
        elements.fullPlayerSheet.classList.remove('open');
        state.isFullPlayerOpen = false;
      } else if (state.activeTab === 'videos') {
        elements.navMusicBtn.click();
      } else {
        showToast("Press back again to exit PulsePlay");
      }
    });
  }

  const hwHomeBtn = document.getElementById('hwHomeBtn');
  if (hwHomeBtn) {
    hwHomeBtn.addEventListener('click', () => {
      if (elements.vlcPlayerScreen.classList.contains('active')) closeVlcPlayer();
      if (state.isFullPlayerOpen) {
        elements.fullPlayerSheet.classList.remove('open');
        state.isFullPlayerOpen = false;
      }
      elements.navMusicBtn.click();
      showToast("PulsePlay Home");
    });
  }

  const hwRecentsBtn = document.getElementById('hwRecentsBtn');
  if (hwRecentsBtn) {
    hwRecentsBtn.addEventListener('click', () => {
      showToast("📱 PulsePlay (com.example) is running");
    });
  }

  const hwRotateBtn = document.getElementById('hwRotateBtn');
  if (hwRotateBtn) {
    hwRotateBtn.addEventListener('click', () => {
      elements.deviceFrame.classList.toggle('landscape');
      const isLand = elements.deviceFrame.classList.contains('landscape');
      showToast(isLand ? "Rotated to Landscape 🔄" : "Rotated to Portrait 📱");
    });
  }

  const hwVolUpBtn = document.getElementById('hwVolUpBtn');
  if (hwVolUpBtn) {
    hwVolUpBtn.addEventListener('click', () => {
      setVolume(state.volume + 0.1);
      showToast(`🔊 Volume: ${Math.round(state.volume * 100)}%`);
    });
  }

  const hwVolDownBtn = document.getElementById('hwVolDownBtn');
  if (hwVolDownBtn) {
    hwVolDownBtn.addEventListener('click', () => {
      setVolume(state.volume - 0.1);
      showToast(`🔉 Volume: ${Math.round(state.volume * 100)}%`);
    });
  }

  const hwPowerBtn = document.getElementById('hwPowerBtn');
  if (hwPowerBtn) {
    hwPowerBtn.addEventListener('click', () => {
      elements.screenViewport.classList.toggle('screen-off');
      const isOff = elements.screenViewport.classList.contains('screen-off');
      showToast(isOff ? "Screen Locked 🔒" : "Screen Woken ☀️");
    });
  }

  const tabPreviewBtn = document.getElementById('tabPreviewBtn');
  const tabCodeBtn = document.getElementById('tabCodeBtn');
  if (tabPreviewBtn && tabCodeBtn) {
    tabPreviewBtn.addEventListener('click', () => {
      tabPreviewBtn.classList.add('active');
      tabCodeBtn.classList.remove('active');
    });
    tabCodeBtn.addEventListener('click', () => {
      tabCodeBtn.classList.add('active');
      tabPreviewBtn.classList.remove('active');
      showToast("Code view: Jetpack Compose in app/src/main/java");
    });
  }

  setupVlcGestures();
  setupVisualizer();
}

// Expose functions globally for inline HTML onclick handlers
window.openVlcPlayer = openVlcPlayer;
window.closeVlcPlayer = closeVlcPlayer;
window.selectAndPlaySong = selectAndPlaySong;
window.toggleFavorite = toggleFavorite;
window.promptDelete = promptDelete;

// --- BOOTSTRAP ---
window.addEventListener('DOMContentLoaded', () => {
  renderSongs();
  renderVideos();
  updatePlaybackUI();
  updateClock();
  setInterval(updateClock, 10000);
  initEvents();
});
