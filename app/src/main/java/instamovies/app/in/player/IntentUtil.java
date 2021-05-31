package instamovies.app.in.player;

import static com.google.android.exoplayer2.util.Assertions.checkNotNull;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.Nullable;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IntentUtil {

    public static final String ACTION_VIEW = "com.google.android.exoplayer.demo.action.VIEW";
    public static final String ACTION_VIEW_LIST = "com.google.android.exoplayer.demo.action.VIEW_LIST";
    public static final String PREFER_EXTENSION_DECODERS_EXTRA = "prefer_extension_decoders";

    public static final String URI_EXTRA = "uri";
    public static final String MIME_TYPE_EXTRA = "mime_type";
    public static final String CLIP_START_POSITION_MS_EXTRA = "clip_start_position_ms";
    public static final String CLIP_END_POSITION_MS_EXTRA = "clip_end_position_ms";

    public static final String AD_TAG_URI_EXTRA = "ad_tag_uri";

    public static final String DRM_SCHEME_EXTRA = "drm_scheme";
    public static final String DRM_LICENSE_URI_EXTRA = "drm_license_uri";
    public static final String DRM_KEY_REQUEST_PROPERTIES_EXTRA = "drm_key_request_properties";
    public static final String DRM_SESSION_FOR_CLEAR_CONTENT = "drm_session_for_clear_content";
    public static final String DRM_MULTI_SESSION_EXTRA = "drm_multi_session";
    public static final String DRM_FORCE_DEFAULT_LICENSE_URI_EXTRA = "drm_force_default_license_uri";

    public static final String SUBTITLE_URI_EXTRA = "subtitle_uri";
    public static final String SUBTITLE_MIME_TYPE_EXTRA = "subtitle_mime_type";
    public static final String SUBTITLE_LANGUAGE_EXTRA = "subtitle_language";

    public static @NotNull List<MediaItem> createMediaItemsFromIntent(@NotNull Intent intent) {
        List<MediaItem> mediaItems = new ArrayList<>();
        if (ACTION_VIEW_LIST.equals(intent.getAction())) {
            int index = 0;
            while (intent.hasExtra(URI_EXTRA + "_" + index)) {
                Uri uri = Uri.parse(intent.getStringExtra(URI_EXTRA + "_" + index));
                mediaItems.add(createMediaItemFromIntent(uri, intent, /* extrasKeySuffix= */ "_" + index));
                index++;
            }
        } else {
            Uri uri = intent.getData();
            mediaItems.add(createMediaItemFromIntent(uri, intent, /* extrasKeySuffix= */ ""));
        }
        return mediaItems;
    }

    private static @NotNull MediaItem createMediaItemFromIntent(Uri uri, @NotNull Intent intent, String extrasKeySuffix) {
        @Nullable String mimeType = intent.getStringExtra(MIME_TYPE_EXTRA + extrasKeySuffix);
        MediaItem.Builder builder = new MediaItem.Builder()
                .setUri(uri)
                .setMimeType(mimeType)
                .setAdTagUri(intent.getStringExtra(AD_TAG_URI_EXTRA + extrasKeySuffix))
                .setSubtitles(createSubtitlesFromIntent(intent, extrasKeySuffix))
                .setClipStartPositionMs(intent.getLongExtra(CLIP_START_POSITION_MS_EXTRA + extrasKeySuffix, 0))
                .setClipEndPositionMs(intent.getLongExtra(CLIP_END_POSITION_MS_EXTRA + extrasKeySuffix, C.TIME_END_OF_SOURCE));
        return populateDrmPropertiesFromIntent(builder, intent, extrasKeySuffix).build();
    }

    private static List<MediaItem.Subtitle> createSubtitlesFromIntent(@NotNull Intent intent, String extrasKeySuffix) {
        if (!intent.hasExtra(SUBTITLE_URI_EXTRA + extrasKeySuffix)) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new MediaItem.Subtitle(
                Uri.parse(intent.getStringExtra(SUBTITLE_URI_EXTRA + extrasKeySuffix)),
                checkNotNull(intent.getStringExtra(SUBTITLE_MIME_TYPE_EXTRA + extrasKeySuffix)),
                intent.getStringExtra(SUBTITLE_LANGUAGE_EXTRA + extrasKeySuffix),
                C.SELECTION_FLAG_DEFAULT));
    }

    @Contract("_, _, _ -> param1")
    private static MediaItem.Builder populateDrmPropertiesFromIntent(MediaItem.Builder builder, @NotNull Intent intent, String extrasKeySuffix) {
        String schemeKey = DRM_SCHEME_EXTRA + extrasKeySuffix;
        @Nullable String drmSchemeExtra = intent.getStringExtra(schemeKey);
        if (drmSchemeExtra == null) {
            return builder;
        }
        Map<String, String> headers = new HashMap<>();
        @Nullable
        String[] keyRequestPropertiesArray =
                intent.getStringArrayExtra(DRM_KEY_REQUEST_PROPERTIES_EXTRA + extrasKeySuffix);
        if (keyRequestPropertiesArray != null) {
            for (int i = 0; i < keyRequestPropertiesArray.length; i += 2) {
                headers.put(keyRequestPropertiesArray[i], keyRequestPropertiesArray[i + 1]);
            }
        }
        builder.setDrmUuid(Util.getDrmUuid(Util.castNonNull(drmSchemeExtra)))
                .setDrmLicenseUri(intent.getStringExtra(DRM_LICENSE_URI_EXTRA + extrasKeySuffix))
                .setDrmMultiSession(intent.getBooleanExtra(DRM_MULTI_SESSION_EXTRA + extrasKeySuffix, false))
                .setDrmForceDefaultLicenseUri(intent.getBooleanExtra(DRM_FORCE_DEFAULT_LICENSE_URI_EXTRA + extrasKeySuffix, false))
                .setDrmLicenseRequestHeaders(headers);
        if (intent.getBooleanExtra(DRM_SESSION_FOR_CLEAR_CONTENT + extrasKeySuffix, false)) {
            builder.setDrmSessionForClearTypes(ImmutableList.of(C.TRACK_TYPE_VIDEO, C.TRACK_TYPE_AUDIO));
        }
        return builder;
    }

    private static void addDrmConfigurationToIntent(MediaItem.@NotNull DrmConfiguration drmConfiguration, @NotNull Intent intent, String extrasKeySuffix) {
        intent.putExtra(DRM_SCHEME_EXTRA + extrasKeySuffix, drmConfiguration.uuid.toString());
        intent.putExtra(DRM_LICENSE_URI_EXTRA + extrasKeySuffix, drmConfiguration.licenseUri != null ? drmConfiguration.licenseUri.toString() : null);
        intent.putExtra(DRM_MULTI_SESSION_EXTRA + extrasKeySuffix, drmConfiguration.multiSession);
        intent.putExtra(DRM_FORCE_DEFAULT_LICENSE_URI_EXTRA + extrasKeySuffix, drmConfiguration.forceDefaultLicenseUri);

        String[] drmKeyRequestProperties = new String[drmConfiguration.requestHeaders.size() * 2];
        int index = 0;
        for (Map.Entry<String, String> entry : drmConfiguration.requestHeaders.entrySet()) {
            drmKeyRequestProperties[index++] = entry.getKey();
            drmKeyRequestProperties[index++] = entry.getValue();
        }
        intent.putExtra(DRM_KEY_REQUEST_PROPERTIES_EXTRA + extrasKeySuffix, drmKeyRequestProperties);

        List<Integer> drmSessionForClearTypes = drmConfiguration.sessionForClearTypes;
        if (!drmSessionForClearTypes.isEmpty()) {
            Assertions.checkState(drmSessionForClearTypes.size() == 2 && drmSessionForClearTypes.contains(C.TRACK_TYPE_VIDEO) && drmSessionForClearTypes.contains(C.TRACK_TYPE_AUDIO));
            intent.putExtra(DRM_SESSION_FOR_CLEAR_CONTENT + extrasKeySuffix, true);
        }
    }

    private static void addClippingPropertiesToIntent(MediaItem.@NotNull ClippingProperties clippingProperties, Intent intent, String extrasKeySuffix) {
        if (clippingProperties.startPositionMs != 0) {
            intent.putExtra(CLIP_START_POSITION_MS_EXTRA + extrasKeySuffix, clippingProperties.startPositionMs);
        }
        if (clippingProperties.endPositionMs != C.TIME_END_OF_SOURCE) {
            intent.putExtra(CLIP_END_POSITION_MS_EXTRA + extrasKeySuffix, clippingProperties.endPositionMs);
        }
    }
}