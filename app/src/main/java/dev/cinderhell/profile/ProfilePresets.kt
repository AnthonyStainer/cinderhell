package dev.cinderhell.profile

internal enum class PresetId(val wireValue: String) {
    ORIGINAL("original"),
    ENHANCED("enhanced"),
    HANDHELD("handheld"),
}

internal data class ProfilePreset(
    val id: PresetId,
    val version: Int,
    val displayName: String,
    val description: String,
    val targetRefreshRate: Int,
    val compatibility: String?,
    val settings: LinkedHashMap<String, Int>,
)

internal object ProfilePresets {
    const val VERSION = 1

    val original = ProfilePreset(
        id = PresetId.ORIGINAL,
        version = VERSION,
        displayName = "Original",
        description = "Classic presentation and conservative behavior.",
        targetRefreshRate = 60,
        compatibility = "vanilla",
        settings = linkedMapOf(
            "current_video_height" to 200,
            "dynamic_resolution" to 0,
            "correct_aspect_ratio" to 1,
            "widescreen" to 0,
            "uncapped" to 0,
            "fpslimit" to 35,
            "smooth_scaling" to 0,
            "freelook" to 0,
            "direct_vertical_aiming" to 0,
            "autorun" to 0,
            "screenblocks" to 10,
            "hud_anchoring" to 1,
            "joy_enable" to 1,
            "joy_camera_inner_deadzone" to 15,
            "joy_camera_curve" to 20,
        ),
    )

    val enhanced = ProfilePreset(
        id = PresetId.ENHANCED,
        version = VERSION,
        displayName = "Enhanced",
        description = "Widescreen, smooth uncapped rendering, and modern aiming.",
        targetRefreshRate = 120,
        compatibility = null,
        settings = linkedMapOf(
            "current_video_height" to 600,
            "dynamic_resolution" to 1,
            "correct_aspect_ratio" to 1,
            "widescreen" to 1,
            "uncapped" to 1,
            "fpslimit" to 0,
            "smooth_scaling" to 1,
            "freelook" to 1,
            "direct_vertical_aiming" to 1,
            "autorun" to 1,
            "screenblocks" to 10,
            "hud_anchoring" to 2,
            "snd_hrtf" to 1,
            "snd_resampler" to 1,
            "joy_enable" to 1,
            "joy_movement_inner_deadzone" to 15,
            "joy_camera_inner_deadzone" to 15,
            "joy_movement_curve" to 10,
            "joy_camera_curve" to 20,
            "joy_movement_outer_deadzone" to 2,
            "joy_camera_outer_deadzone" to 2,
        ),
    )

    val handheld = ProfilePreset(
        id = PresetId.HANDHELD,
        version = VERSION,
        displayName = "Handheld",
        description = "Battery-conscious rendering and tuned handheld controls.",
        targetRefreshRate = 120,
        compatibility = null,
        settings = linkedMapOf(
            "current_video_height" to 400,
            "dynamic_resolution" to 1,
            "correct_aspect_ratio" to 1,
            "widescreen" to 1,
            "uncapped" to 1,
            "fpslimit" to 120,
            "smooth_scaling" to 1,
            "freelook" to 1,
            "direct_vertical_aiming" to 1,
            "autorun" to 1,
            "screenblocks" to 10,
            "hud_anchoring" to 2,
            "snd_hrtf" to 1,
            "snd_resampler" to 1,
            "joy_enable" to 1,
            "joy_stick_layout" to 1,
            "joy_forward_sensitivity" to 10,
            "joy_strafe_sensitivity" to 10,
            "joy_turn_speed" to 240,
            "joy_look_speed" to 150,
            "joy_outer_turn_speed" to 60,
            "joy_outer_look_speed" to 30,
            "joy_outer_ramp_time" to 20,
            "joy_movement_curve" to 10,
            "joy_camera_curve" to 20,
            "joy_movement_deadzone_type" to 1,
            "joy_camera_deadzone_type" to 1,
            "joy_movement_inner_deadzone" to 15,
            "joy_camera_inner_deadzone" to 15,
            "joy_movement_outer_deadzone" to 2,
            "joy_camera_outer_deadzone" to 2,
            "joy_trigger_deadzone" to 15,
        ),
    )

    val all: List<ProfilePreset> = listOf(original, enhanced, handheld)

    fun require(wireValue: String, version: Int): ProfilePreset {
        val preset = all.singleOrNull { it.id.wireValue == wireValue }
            ?: throw IllegalArgumentException("Unknown presentation preset.")
        require(preset.version == version) {
            "This profile uses an unavailable preset version."
        }
        return preset
    }
}
