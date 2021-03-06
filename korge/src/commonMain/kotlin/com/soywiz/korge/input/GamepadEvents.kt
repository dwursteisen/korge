package com.soywiz.korge.input

import com.soywiz.kds.*
import com.soywiz.korge.component.*
import com.soywiz.korge.view.*
import com.soywiz.korio.async.*
import com.soywiz.korev.*
import com.soywiz.korge.internal.fastForEach

class GamePadEvents(override val view: View) : GamepadComponent {
	val gamepads = GamePadUpdateEvent()
	val updated = Signal<GamePadUpdateEvent>()
	val stick = Signal<GamePadStickEvent>()
	val button = Signal<GamePadButtonEvent>()
	val connection = Signal<GamePadConnectionEvent>()

	fun stick(playerId: Int, stick: GameStick, callback: (x: Double, y: Double) -> Unit) {
		stick { e -> if (e.gamepad == playerId && e.stick == stick) callback(e.x, e.y) }
	}

	fun stick(callback: (playerId: Int, stick: GameStick, x: Double, y: Double) -> Unit) {
		stick { e -> callback(e.gamepad, e.stick, e.x, e.y) }
	}

	fun button(callback: (playerId: Int, pressed: Boolean, button: GameButton, value: Double) -> Unit) {
		button { e ->
			callback(e.gamepad, e.type == GamePadButtonEvent.Type.DOWN, e.button, e.value)
		}
	}

	fun button(playerId: Int, callback: (pressed: Boolean, button: GameButton, value: Double) -> Unit) {
		button { e ->
			if (e.gamepad == playerId) callback(e.type == GamePadButtonEvent.Type.DOWN, e.button, e.value)
		}
	}

	fun down(playerId: Int, button: GameButton, callback: () -> Unit) {
		button { e ->
			if (e.gamepad == playerId && e.button == button && e.type == GamePadButtonEvent.Type.DOWN) callback()
		}
	}

	fun up(playerId: Int, button: GameButton, callback: () -> Unit) {
		button { e ->
			if (e.gamepad == playerId && e.button == button && e.type == GamePadButtonEvent.Type.UP) callback()
		}
	}

	fun connected(callback: (playerId: Int) -> Unit) {
		connection { e ->
			if (e.type == GamePadConnectionEvent.Type.CONNECTED) callback(e.gamepad)
		}
	}

	fun disconnected(callback: (playerId: Int) -> Unit) {
		connection { e ->
			if (e.type == GamePadConnectionEvent.Type.DISCONNECTED) callback(e.gamepad)
		}
	}

	private val oldGamepads = GamePadUpdateEvent()

	private val stickEvent = GamePadStickEvent()
	private val buttonEvent = GamePadButtonEvent()

	override fun onGamepadEvent(views: Views, event: GamePadUpdateEvent) {
		gamepads.copyFrom(event)
		// Compute diff
		for (gamepadIndex in 0 until event.gamepadsLength) {
			val gamepad = event.gamepads[gamepadIndex]
			val oldGamepad = this.oldGamepads.gamepads[gamepadIndex]
			GameButton.BUTTONS.fastForEach { button ->
				if (gamepad[button] != oldGamepad[button]) {
					button(buttonEvent.apply {
						this.gamepad = gamepad.index
						this.type = if (gamepad[button] != 0.0) GamePadButtonEvent.Type.DOWN else GamePadButtonEvent.Type.UP
						this.button = button
						this.value = gamepad[button]
					})
				}
			}
			GameStick.STICKS.fastForEach { stick ->
				val vector = gamepad[stick]
				if (vector != oldGamepad[stick]) {
					stick(stickEvent.apply {
						this.gamepad = gamepad.index
						this.stick = stick
						this.x = vector.x
						this.y = vector.y
					})
				}
			}
		}
		oldGamepads.copyFrom(event)
		updated(event)
	}

	override fun onGamepadEvent(views: Views, event: GamePadButtonEvent) {
		button(event)
	}

	override fun onGamepadEvent(views: Views, event: GamePadStickEvent) {
		stick(event)
	}

	override fun onGamepadEvent(views: Views, event: GamePadConnectionEvent) {
		connection(event)
	}
}

val View.gamepad by Extra.PropertyThis<View, GamePadEvents> { this.getOrCreateComponent { GamePadEvents(this) } }
inline fun <T> View.gamepad(callback: GamePadEvents.() -> T): T = gamepad.run(callback)
