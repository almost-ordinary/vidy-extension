(function() {
    console.log("[FrameTune Vidy] Injected TV D-Pad Bridge");

    // CSS for high-visibility TV remote focus highlights
    const style = document.createElement("style");
    style.innerHTML = `
        .frametune-tv-focus {
            outline: 3px solid #E50914 !important;
            outline-offset: 3px !important;
            box-shadow: 0 0 16px rgba(229, 9, 20, 0.9) !important;
            transform: scale(1.08) !important;
            transition: transform 0.15s ease, outline 0.15s ease !important;
            z-index: 99999 !important;
        }
    `;
    document.head.appendChild(style);

    let focusIndex = 0;
    let focusableElements = [];

    function updateFocusables() {
        const selector = "button, [role='button'], input, select, a, [tabindex='0'], .server-item, .episode-item, li";
        focusableElements = Array.from(document.querySelectorAll(selector)).filter(el => {
            const rect = el.getBoundingClientRect();
            return rect.width > 0 && rect.height > 0 && window.getComputedStyle(el).visibility !== "hidden";
        });
    }

    function applyFocus(index) {
        focusableElements.forEach(el => el.classList.remove("frametune-tv-focus"));
        if (focusableElements.length === 0) return;
        focusIndex = Math.max(0, Math.min(index, focusableElements.length - 1));
        const el = focusableElements[focusIndex];
        if (el) {
            el.classList.add("frametune-tv-focus");
            el.focus();
            el.scrollIntoView({ behavior: "smooth", block: "nearest" });
        }
    }

    function getVideo() {
        return document.querySelector("video");
    }

    window.TvBridge = {
        togglePlayPause: function() {
            const video = getVideo();
            if (video) {
                if (video.paused) {
                    video.play();
                } else {
                    video.pause();
                }
                return true;
            }
            // Fallback click on active element or main play button
            const playBtn = document.querySelector("button[aria-label*='Play'], button[aria-label*='Pause'], .vds-play-button");
            if (playBtn) {
                playBtn.click();
                return true;
            }
            return false;
        },

        seek: function(seconds) {
            const video = getVideo();
            if (video) {
                video.currentTime = Math.max(0, Math.min(video.duration || Infinity, video.currentTime + seconds));
                return true;
            }
            return false;
        },

        moveFocus: function(direction) {
            updateFocusables();
            if (focusableElements.length === 0) return;

            if (direction === "next") {
                focusIndex = (focusIndex + 1) % focusableElements.length;
            } else if (direction === "prev") {
                focusIndex = (focusIndex - 1 + focusableElements.length) % focusableElements.length;
            }
            applyFocus(focusIndex);
        },

        clickFocused: function() {
            updateFocusables();
            const el = focusableElements[focusIndex];
            if (el) {
                el.click();
                return true;
            }
            return false;
        },

        closeModal: function() {
            // Check for close buttons in dialogs/dropdowns
            const closeBtn = document.querySelector("[aria-label*='Close'], .close-modal, .menu-close");
            if (closeBtn) {
                closeBtn.click();
                return true;
            }
            return false;
        }
    };

    // Auto-focus play button on load
    setTimeout(() => {
        updateFocusables();
        applyFocus(0);
    }, 1500);
})();
