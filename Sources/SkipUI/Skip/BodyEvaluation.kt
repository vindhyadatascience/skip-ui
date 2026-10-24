// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import skip.model.StateTracking

/** Identity must not call a Swift class's custom equals/hashCode implementation. */
private class BodyInputReference(private val reference: Any) {
    override fun equals(other: Any?): Boolean = other is BodyInputReference && reference === other.reference
    override fun hashCode(): Int = System.identityHashCode(reference)
}

/** Used only for fields whose reference semantics the compiler has proved. */
fun bodyInputReference(reference: Any): Any = BodyInputReference(reference)

/**
 * The compiler supplies keys only for immutable, completely known stored inputs. The retained
 * builder captures an equivalent old Self, never a view constructor or an evaluated result.
 * ComposeBuilder.Evaluate discards modifier/composer/scope itself; stateSaver is preserved.
 */
@Composable
fun evaluateImmutableViewBody(context: ComposeContext, options: Int, inputKeys: kotlin.Array<Any?>,
    bodyFactory: () -> View): kotlin.collections.List<Renderable> {
    val normalized = context.content()
    val keys = kotlin.arrayOfNulls<Any?>(inputKeys.size + 2)
    inputKeys.copyInto(keys)
    keys[inputKeys.size] = bodyInputReference(normalized.stateSaver)
    keys[inputKeys.size + 1] = options
    val holder = remember(*keys) { BodyEvaluationHolder(bodyFactory(), normalized, options) }
    holder.evaluate()
    // Read after the restartable Unit call. Its fresh result invalidates this parent when a
    // snapshot/environment dependency changes independently of the parent's input keys.
    return holder.readResult()
}

@Stable
private class BodyEvaluationHolder(private val body: View, private val context: ComposeContext,
    private val options: Int) {
    private val output = mutableStateOf<kotlin.collections.List<Renderable>>(emptyList(), neverEqualPolicy())
    fun readResult(): kotlin.collections.List<Renderable> = output.value

    private var evaluationID = 0L

    @Composable
    fun evaluate() {
        // A failed recomposition does not abandon an observer retained from a prior pass.
        // Allocate a fresh candidate for each actual Unit restart, not each parent call.
        val tracking = remember(++evaluationID) { BodyTrackingExecution() }
        tracking.begin()
        val renderables = body.Evaluate(context, options)
        tracking.complete()
        output.value = renderables
    }
}

/** Compose dispatches onAbandoned for fresh remembers when composition/recomposition fails. */
private class BodyTrackingExecution : RememberObserver {
    private var active = false
    fun begin() { active = true; StateTracking.pushBody() }
    fun complete() {
        if (active) { active = false; StateTracking.popBody() }
    }
    override fun onRemembered() { }
    override fun onForgotten() = complete()
    override fun onAbandoned() = complete()
}
