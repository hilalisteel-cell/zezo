package com.kidstok.app

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.File

object OfflinePage {
    fun build(context: Context): String {
        val feed = File(context.filesDir, AppConfig.FEED_FILE)
        val mediaDir = File(context.filesDir, AppConfig.CACHE_DIR)
        if (!feed.exists()) return emptyState()

        return try {
            val root = JSONObject(feed.readText(Charsets.UTF_8))
            val videos = root.optJSONArray("videos")
            val cards = StringBuilder()

            if (videos != null) {
                for (i in 0 until videos.length()) {
                    val v = videos.optJSONObject(i) ?: continue
                    val id = v.optLong("id")
                    val creatorId = v.optLong("creator_id")
                    val title = esc(v.optString("title"))
                    val creator = esc(v.optString("creator_name"))
                    val creatorInitial = esc(v.optString("creator_name").take(1))
                    val desc = esc(v.optString("description"))
                    val views = fmt(v.optLong("views"))
                    val followers = fmt(v.optLong("followers_count"))
                    val likes = fmt(v.optLong("likes_count"))
                    val dislikes = fmt(v.optLong("dislikes_count"))
                    val verified = v.optBoolean("creator_verified", false)
                    val verifiedHtml = if (verified) "<span class=\"verified\">✓</span>" else ""

                    val videoFile = findLocal(mediaDir, "video_" + id + ".") ?: continue
                    val videoUri = esc(Uri.fromFile(videoFile).toString())
                    val avatarFile = findLocal(mediaDir, "avatar_" + creatorId + ".")
                    val avatarHtml = if (avatarFile != null) {
                        "<img src=\"" + esc(Uri.fromFile(avatarFile).toString()) + "\" alt=\"\">"
                    } else creatorInitial

                    cards.append(
                        """
                        <section class="reel" data-video-id="$id">
                          <video class="reel-video" src="$videoUri" preload="auto" autoplay muted playsinline loop></video>
                          <div class="video-start-cover" aria-hidden="true"></div>
                          <div class="reel-gradient"></div>
                          <div class="reel-info">
                            <div class="creator-row">
                              <div class="creator-link">
                                <span class="creator-avatar">$avatarHtml</span>
                                <span class="creator-name-text">@$creator$verifiedHtml</span>
                              </div>
                              <button class="follow-btn" type="button">متابعة</button>
                            </div>
                            <h3>$title</h3>
                            <p>$desc</p>
                            <div class="mobile-meta"><span>$followers متابع</span> · <span>$views مشاهدة</span></div>
                          </div>
                          <div class="action-rail">
                            <div class="action-item"><button class="action-btn reaction-btn like-btn" type="button"><span class="action-icon">♥</span></button><div class="action-count">$likes</div></div>
                            <div class="action-item"><button class="action-btn reaction-btn dislike-btn" type="button"><span class="action-icon">👎</span></button><div class="action-count">$dislikes</div></div>
                            <div class="action-item"><button class="action-btn share-btn" type="button"><span class="action-icon">↗</span></button><div class="action-label">مشاركة</div></div>
                            <div class="action-item"><button class="action-btn rotate-btn" type="button"><span class="action-icon">⟳</span></button><div class="action-label">أفقي</div></div>
                            <div class="action-item"><button class="action-btn mute-btn" type="button"><span class="action-icon">🔊</span></button><div class="action-label">الصوت</div></div>
                          </div>
                        </section>
                        """.trimIndent()
                    )
                }
            }

            if (cards.isEmpty()) {
                return emptyState("اتصل بالإنترنت مرة واحدة وانتظر اكتمال تنزيل بعض الفيديوهات، وبعدها ستعمل هنا بدون إنترنت.")
            }
            template(cards.toString())
        } catch (_: Throwable) {
            emptyState()
        }
    }

    private fun findLocal(dir: File, prefix: String): File? =
        dir.listFiles()?.firstOrNull { it.name.startsWith(prefix) && it.isFile }

    private fun esc(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

    private fun fmt(n: Long): String = when {
        n >= 1_000_000 -> ((n / 100_000) / 10.0).toString().removeSuffix(".0") + "م"
        n >= 1_000 -> ((n / 100) / 10.0).toString().removeSuffix(".0") + "ألف"
        else -> n.toString()
    }

    private fun template(cards: String) = """
<!doctype html>
<html lang="ar" dir="rtl">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">
<style>
*{box-sizing:border-box}
html,body{margin:0;padding:0;background:#000;color:#fff;font-family:Arial,Tahoma,sans-serif;height:100%;overflow:hidden}
.reels-header{position:fixed;top:2cm;right:0;left:0;z-index:120;min-height:64px;display:flex;align-items:center;justify-content:space-between;gap:12px;padding:10px 14px;background:linear-gradient(180deg,rgba(10,10,12,.90),rgba(10,10,12,.35),transparent);pointer-events:none}
.brand{font-size:20px;font-weight:900;white-space:nowrap;color:#fff}.header-actions{display:flex;align-items:center;gap:10px;pointer-events:auto}
.header-btn{color:#fff;font-size:13px;font-weight:800;padding:9px 14px;border-radius:20px;background:rgba(0,0,0,.34);border:1px solid rgba(255,255,255,.15)}
.search-toggle{width:39px;height:39px;border-radius:50%;border:1px solid rgba(255,255,255,.22);background:rgba(0,0,0,.35);color:#fff;font-size:23px;display:grid;place-items:center}
.reels{height:100svh;overflow-y:auto;scroll-snap-type:y mandatory;background:#000}
.reel{position:relative;height:100svh;scroll-snap-align:start;overflow:hidden;background:#000}
.reel-video{width:100%;height:100%;object-fit:contain;background:#000;display:block}
.reel-video::-webkit-media-controls-start-playback-button,.reel-video::-webkit-media-controls-overlay-play-button{display:none!important;-webkit-appearance:none!important;opacity:0!important}
.video-start-cover{position:absolute;inset:0;z-index:2;background:#000;pointer-events:none;opacity:1;transition:opacity .12s ease}
.video-start-cover.hidden{opacity:0;visibility:hidden}
.reel-gradient{position:absolute;z-index:3;inset:auto 0 0;height:47%;background:linear-gradient(transparent,rgba(0,0,0,.84));pointer-events:none}
.reel-info{position:absolute;right:16px;left:88px;bottom:82px;z-index:5}
.creator-row{display:flex;align-items:center;gap:9px;flex-wrap:wrap}.creator-link{color:#fff;font-weight:900;display:inline-flex;align-items:center;gap:8px;min-width:0}
.creator-avatar{width:38px;height:38px;border-radius:50%;flex:0 0 38px;overflow:hidden;display:grid;place-items:center;background:linear-gradient(135deg,#1496f5,#7357ff,#ee328e);color:#fff;font-size:15px;font-weight:900;border:2px solid rgba(255,255,255,.92);box-shadow:0 3px 14px rgba(0,0,0,.35)}
.creator-avatar img{width:100%;height:100%;object-fit:cover;display:block}.creator-name-text{display:inline-flex;align-items:center;gap:3px;max-width:180px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.verified{display:inline-grid;place-items:center;width:18px;height:18px;border-radius:50%;background:#1687ff;font-size:11px;margin-right:4px}
.follow-btn{border:0;border-radius:18px;padding:8px 13px;font-weight:800;background:#eee;color:#111}.follow-btn.active{background:#333;color:#fff}
.reel-info h3{margin:10px 0 5px}.reel-info p{margin:0;color:#ddd;line-height:1.55}.mobile-meta{margin-top:8px;font-size:13px;color:#ddd}
.action-rail{position:absolute;left:11px;bottom:82px;z-index:7;display:flex;flex-direction:column;gap:9px;align-items:center}
.action-item{display:flex;flex-direction:column;align-items:center;gap:4px}.action-btn{width:52px;height:52px;border:1px solid rgba(255,255,255,.12);border-radius:50%;background:rgba(20,20,22,.70);color:#fff;font-size:21px;display:grid;place-items:center;backdrop-filter:blur(10px);box-shadow:0 5px 18px rgba(0,0,0,.28)}
.action-btn.active{background:linear-gradient(135deg,#1687ff,#7357ff)}.action-count{font-size:11px;font-weight:900;color:#fff;min-height:13px}.action-label{font-size:10px;font-weight:800;color:#fff}
@media(max-width:420px){.reel-info{right:12px;left:78px;bottom:74px}.action-rail{left:8px;bottom:72px;gap:7px}.action-btn{width:48px;height:48px;font-size:20px}}
</style>
</head>
<body>
<header class="reels-header">
  <div class="brand">Kids-tok ▶</div>
  <div class="header-actions"><div class="search-toggle">⌕</div><div class="header-btn">القنوات</div><div class="header-btn">خروج</div></div>
</header>
<main class="reels" id="reels">$cards</main>
<script>
const reels=[...document.querySelectorAll('.reel')];
const videos=[...document.querySelectorAll('.reel-video')];
const ratios=new Map();
function stopOthers(except){videos.forEach(function(v){if(v!==except&&!v.paused)v.pause();});}
function activate(){
  var best=null,bestRatio=0;
  reels.forEach(function(r){var x=ratios.get(r)||0;if(x>bestRatio){bestRatio=x;best=r;}});
  if(!best||bestRatio<0.60){stopOthers(null);return;}
  var v=best.querySelector('.reel-video'); if(!v)return;
  stopOthers(v);
  if(v.paused){
    v.muted=true;
    v.dataset.autoMuted='1';
    var attempt=v.play();
    if(attempt&&typeof attempt.catch==='function'){
      attempt.catch(function(){
        setTimeout(function(){
          v.muted=true;
          v.dataset.autoMuted='1';
          v.play().catch(function(){});
        },120);
      });
    }
  }
}
const observer=new IntersectionObserver(function(entries){
  entries.forEach(function(e){ratios.set(e.target,e.isIntersecting?e.intersectionRatio:0);});
  requestAnimationFrame(activate);
},{threshold:[0,.25,.5,.6,.75,.9,1]});
reels.forEach(function(r){observer.observe(r);});
setTimeout(function(){
  if(reels[0]){
    ratios.set(reels[0],1);
    activate();
  }
},80);
videos.forEach(function(v){
  var cover=v.parentElement.querySelector('.video-start-cover');
  function onPlaying(){
    if(cover)cover.classList.add('hidden');
    if(v.dataset.autoMuted==='1'){
      setTimeout(function(){
        if(!v.paused){
          v.muted=false;
          v.dataset.autoMuted='0';
          var icon=v.closest('.reel')?.querySelector('.mute-btn .action-icon');
          if(icon)icon.textContent='🔊';
        }
      },180);
    }
  }
  v.addEventListener('playing',onPlaying);
  v.addEventListener('canplay',function(){
    var reel=v.closest('.reel');
    var ratio=reel?(ratios.get(reel)||0):0;
    if(ratio>=0.60&&v.paused){
      v.muted=true;
      v.dataset.autoMuted='1';
      v.play().catch(function(){});
    }
  });
  v.addEventListener('play',function(){stopOthers(v);});
  v.addEventListener('click',function(){
    if(v.paused){
      v.muted=true;
      v.dataset.autoMuted='1';
      v.play().catch(function(){});
    }
  });
});
document.addEventListener('click',function(e){
  var reaction=e.target.closest('.reaction-btn');
  if(reaction){reaction.classList.toggle('active');return;}
  var follow=e.target.closest('.follow-btn');
  if(follow){follow.classList.toggle('active');follow.textContent=follow.classList.contains('active')?'متابَع':'متابعة';return;}
  var mute=e.target.closest('.mute-btn');
  if(mute){var reel=mute.closest('.reel');var v=reel?reel.querySelector('video'):null;if(v){v.muted=!v.muted;var i=mute.querySelector('.action-icon');if(i)i.textContent=v.muted?'🔇':'🔊';}return;}
  var share=e.target.closest('.share-btn');
  if(share){alert('المشاركة تحتاج الإنترنت.');return;}
});
document.addEventListener('visibilitychange',function(){if(document.hidden)stopOthers(null);else activate();});
</script>
</body>
</html>
""".trimIndent()

    private fun emptyState(message: String = "لا توجد نسخة محفوظة على الجهاز حتى الآن.") = """
<!doctype html><html lang="ar" dir="rtl"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<style>body{margin:0;background:#000;color:#fff;font-family:Arial,Tahoma,sans-serif;min-height:100vh;display:grid;place-items:center;text-align:center}.box{max-width:520px;padding:30px}.play{font-size:48px}.box h1{font-size:28px}.box p{color:#ccc;line-height:1.9}</style>
</head><body><div class="box"><div class="play">▶</div><h1>Kids-tok</h1><p>$message</p></div></body></html>
""".trimIndent()
}
