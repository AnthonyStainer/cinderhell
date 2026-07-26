package dev.cinderhell.input

internal data class GameplayControl(
    val control: String,
    val action: String,
    val woofBinding: String,
)

internal object HandheldGameplayMapping {
    val controls = listOf(
        GameplayControl("Left stick", "Move and strafe", "Woof default analogue movement"),
        GameplayControl("Right stick", "Turn and look", "Woof default analogue camera"),
        GameplayControl("Right trigger", "Fire", "input_fire"),
        GameplayControl("A / South", "Use and confirm", "input_use / menu confirm"),
        GameplayControl("B / East", "Menu back", "menu cancel"),
        GameplayControl("Left shoulder", "Previous weapon", "input_prevweapon"),
        GameplayControl("Right shoulder", "Next weapon", "input_nextweapon"),
        GameplayControl("Y / North", "Automap", "input_map"),
        GameplayControl("D-pad", "Navigate menus", "menu directions"),
        GameplayControl("Start / Android Back", "Open or close menu", "menu activate / Escape"),
    )
}
