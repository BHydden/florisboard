/*
 * Copyright (C) 2022 Patrick Goldinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.app.ui.settings.theme

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.components.FlorisChip
import dev.patrickgold.florisboard.app.ui.components.FlorisDropdownMenu
import dev.patrickgold.florisboard.app.ui.components.FlorisHyperlinkText
import dev.patrickgold.florisboard.app.ui.components.FlorisIconButton
import dev.patrickgold.florisboard.app.ui.components.FlorisOutlinedBox
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.florisboard.app.ui.components.FlorisTextButton
import dev.patrickgold.florisboard.app.ui.components.florisHorizontalScroll
import dev.patrickgold.florisboard.app.ui.components.rippleClickable
import dev.patrickgold.florisboard.common.kotlin.curlyFormat
import dev.patrickgold.florisboard.ime.nlp.NATIVE_NULLPTR
import dev.patrickgold.florisboard.ime.text.key.InputMode
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.ime.theme.FlorisImeUiSpec
import dev.patrickgold.florisboard.ime.theme.ThemeExtensionComponentEditor
import dev.patrickgold.florisboard.res.cache.CacheManager
import dev.patrickgold.florisboard.res.io.readJson
import dev.patrickgold.florisboard.res.io.subFile
import dev.patrickgold.florisboard.snygg.Snygg
import dev.patrickgold.florisboard.snygg.SnyggLevel
import dev.patrickgold.florisboard.snygg.SnyggRule
import dev.patrickgold.florisboard.snygg.SnyggStylesheet
import dev.patrickgold.florisboard.snygg.SnyggStylesheetEditor
import dev.patrickgold.florisboard.snygg.SnyggStylesheetJsonConfig
import dev.patrickgold.florisboard.snygg.value.SnyggDefinedVarValue
import dev.patrickgold.florisboard.snygg.value.SnyggShapeValue
import dev.patrickgold.florisboard.snygg.value.SnyggSolidColorValue
import dev.patrickgold.florisboard.snygg.value.SnyggSpSizeValue
import dev.patrickgold.florisboard.snygg.value.SnyggValue
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog
import dev.patrickgold.jetpref.material.ui.JetPrefListItem

private val SnyggEmptyRuleForAdding = SnyggRule(element = "- select -")
private val IntListSaver = Saver<SnapshotStateList<Int>, ArrayList<Int>>(
    save = { ArrayList(it) },
    restore = { it.toMutableStateList() },
)

@Composable
fun ThemeEditorScreen(
    workspace: CacheManager.ExtEditorWorkspace<*>,
    editor: ThemeExtensionComponentEditor,
) = FlorisScreen {
    title = stringRes(R.string.ext__editor__edit_component__title_theme)

    val stylesheetEditor = remember {
        editor.stylesheetEditor ?: run {
            val stylesheetPath = editor.stylesheetPath()
            editor.stylesheetPathOnLoad = stylesheetPath
            val stylesheetFile = workspace.dir.subFile(stylesheetPath)
            if (stylesheetFile.exists()) {
                try {
                    stylesheetFile.readJson<SnyggStylesheet>(SnyggStylesheetJsonConfig).edit()
                } catch (e: Throwable) {
                    SnyggStylesheetEditor()
                }
            } else {
                SnyggStylesheetEditor()
            }
        }.also { editor.stylesheetEditor = it }
    }
    var snyggLevel by remember { mutableStateOf(SnyggLevel.ADVANCED) }
    var snyggRuleToEdit by rememberSaveable(saver = SnyggRule.Saver) { mutableStateOf(null) }

    fun handleBackPress() {
        workspace.currentAction = null
    }

    navigationIcon {
        FlorisIconButton(
            onClick = { handleBackPress() },
            icon = painterResource(R.drawable.ic_close),
        )
    }

    actions {
        FlorisIconButton(
            onClick = {
                snyggLevel = when (snyggLevel) {
                    SnyggLevel.BASIC -> SnyggLevel.ADVANCED
                    SnyggLevel.ADVANCED -> SnyggLevel.DEVELOPER
                    SnyggLevel.DEVELOPER -> SnyggLevel.BASIC
                }
            },
            icon = painterResource(R.drawable.ic_language),
        )
    }

    floatingActionButton {
        ExtendedFloatingActionButton(
            icon = { Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = null,
            ) },
            text = { Text(
                text = stringRes(R.string.settings__theme_editor__add_rule),
            ) },
            onClick = { snyggRuleToEdit = SnyggEmptyRuleForAdding },
        )
    }

    // TODO: lazy column??
    content {
        BackHandler {
            handleBackPress()
        }

        if (stylesheetEditor.rules.isEmpty()) {
            Text(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                text = stringRes(R.string.settings__theme_editor__no_rules_defined),
                fontStyle = FontStyle.Italic,
            )
        }
        val definedVariables = remember(stylesheetEditor.rules) {
            stylesheetEditor.rules.firstNotNullOfOrNull { (rule, propertySet) ->
                if (rule.isAnnotation && rule.element == "defines") {
                    propertySet.properties
                } else {
                    null
                }
            } ?: emptyMap()
        }
        for ((rule, propertySet) in stylesheetEditor.rules) key(rule) {
            val isVariablesRule = rule.isAnnotation && rule.element == "defines"
            val propertySetSpec = FlorisImeUiSpec.propertySetSpec(rule.element)
            FlorisOutlinedBox(
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 16.dp)
                    .fillMaxWidth(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SnyggRuleRow(
                        rule = rule,
                        level = snyggLevel,
                        onAddPropertyBtnClick = { },
                    )
                    for ((propertyName, propertyValue) in propertySet.properties) {
                        val propertySpec = propertySetSpec?.propertySpec(propertyName)
                        if (propertySpec != null && propertySpec.level <= snyggLevel || isVariablesRule) {
                            JetPrefListItem(
                                modifier = Modifier.rippleClickable {  },
                                text = translatePropertyName(propertyName, snyggLevel),
                                secondaryText = translatePropertyValue(propertyValue, snyggLevel),
                                trailing = { SnyggValueIcon(propertyValue, definedVariables) },
                            )
                        }
                    }
                }
                if (!isVariablesRule) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                    ) {
                        FlorisTextButton(
                            onClick = { workspace.update { stylesheetEditor.rules.remove(rule) } },
                            icon = painterResource(R.drawable.ic_delete),
                            text = stringRes(R.string.action__delete),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colors.error,
                            ),
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        FlorisTextButton(
                            onClick = { snyggRuleToEdit = rule },
                            icon = painterResource(R.drawable.ic_edit),
                            text = stringRes(R.string.action__edit),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(72.dp))

        val ruleToEdit = snyggRuleToEdit
        if (ruleToEdit != null) {
            EditRuleDialog(
                initRule = ruleToEdit,
                level = snyggLevel,
                onConfirmRule = { oldRule, newRule ->
                    val rules = stylesheetEditor.rules
                    when {
                        oldRule == newRule -> {
                            snyggRuleToEdit = null
                            true
                        }
                        rules.contains(newRule) -> {
                            false
                        }
                        else -> workspace.update {
                            val set = rules.remove(oldRule)
                            if (set != null) {
                                rules[newRule] = set
                                snyggRuleToEdit = null
                                true
                            } else {
                                false
                            }
                        }
                    }
                },
                onDismiss = { snyggRuleToEdit = null },
            )
        }
    }
}

@Composable
private fun SnyggRuleRow(
    rule: SnyggRule,
    level: SnyggLevel,
    onAddPropertyBtnClick: () -> Unit,
) {
    @Composable
    fun Selector(text: String) {
        Text(
            modifier = Modifier
                .background(MaterialTheme.colors.primaryVariant)
                .padding(end = 4.dp),
            text = text,
            style = MaterialTheme.typography.body2,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    @Composable
    fun AttributesList(text: String, list: String) {
        Text(
            text = "$text = $list",
            style = MaterialTheme.typography.body2,
            color = LocalContentColor.current.copy(alpha = 0.56f),
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp, horizontal = 10.dp),
        ) {
            Text(
                text = translateElementName(rule, level),
                style = MaterialTheme.typography.body2,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                if (rule.pressedSelector) {
                    Selector(text = "pressed")
                }
                if (rule.focusSelector) {
                    Selector(text = "focus")
                }
                if (rule.disabledSelector) {
                    Selector(text = "disabled")
                }
            }
            if (rule.codes.isNotEmpty()) {
                AttributesList(text = "codes", list = remember(rule.codes) { rule.codes.toString() })
            }
            if (rule.modes.isNotEmpty()) {
                AttributesList(text = "modes", list = remember(rule.modes) { rule.modes.toString() })
            }
        }
        FlorisTextButton(
            onClick = onAddPropertyBtnClick,
            icon = painterResource(R.drawable.ic_add),
            text = stringRes(R.string.action__add),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colors.secondary,
            ),
        )
    }
}

@Composable
private fun EditRuleDialog(
    initRule: SnyggRule,
    level: SnyggLevel,
    onConfirmRule: (oldRule: SnyggRule, newRule: SnyggRule) -> Boolean,
    onDismiss: () -> Unit,
) {
    val isAddRuleDialog = initRule == SnyggEmptyRuleForAdding
    var showSelectAsError by rememberSaveable { mutableStateOf(false) }
    var showAlreadyExistsError by rememberSaveable { mutableStateOf(false) }

    val possibleElementNames = remember {
        listOf(SnyggEmptyRuleForAdding.element) + FlorisImeUiSpec.elements.keys
    }
    val possibleElementLabels = possibleElementNames.map { translateElementName(it, level) ?: it }
    var elementsExpanded by remember { mutableStateOf(false) }
    var elementsSelectedIndex by rememberSaveable {
        val index = possibleElementNames.indexOf(initRule.element).coerceIn(possibleElementNames.indices)
        mutableStateOf(index)
    }

    val codes = rememberSaveable(saver = IntListSaver) { initRule.codes.toMutableStateList() }
    var editCodeDialogValue by rememberSaveable { mutableStateOf<Int?>(null) }
    val groups = rememberSaveable(saver = IntListSaver) { initRule.groups.toMutableStateList() }
    var modeNormal by rememberSaveable { mutableStateOf(initRule.modes.contains(InputMode.NORMAL.value)) }
    var modeShiftLock by rememberSaveable { mutableStateOf(initRule.modes.contains(InputMode.SHIFT_LOCK.value)) }
    var modeCapsLock by rememberSaveable { mutableStateOf(initRule.modes.contains(InputMode.CAPS_LOCK.value)) }
    var pressedSelector by rememberSaveable { mutableStateOf(initRule.pressedSelector) }
    var focusSelector by rememberSaveable { mutableStateOf(initRule.focusSelector) }
    var disabledSelector by rememberSaveable { mutableStateOf(initRule.disabledSelector) }

    JetPrefAlertDialog(
        title = stringRes(if (isAddRuleDialog) {
            R.string.settings__theme_editor__add_rule
        } else {
            R.string.settings__theme_editor__edit_rule
        }),
        confirmLabel = stringRes(if (isAddRuleDialog) {
            R.string.action__add
        } else {
            R.string.action__apply
        }),
        onConfirm = {
            if (isAddRuleDialog && elementsSelectedIndex == 0) {
                showSelectAsError = true
            } else {
                val newRule = SnyggRule(
                    element = possibleElementNames[elementsSelectedIndex],
                    codes = codes.toList(),
                    groups = groups.toList(),
                    modes = buildList {
                        if (modeNormal) { add(InputMode.NORMAL.value) }
                        if (modeShiftLock) { add(InputMode.SHIFT_LOCK.value) }
                        if (modeCapsLock) { add(InputMode.CAPS_LOCK.value) }
                    },
                    pressedSelector = pressedSelector,
                    focusSelector = focusSelector,
                    disabledSelector = disabledSelector,
                )
                if (!onConfirmRule(initRule, newRule)) {
                    showAlreadyExistsError = true
                }
            }
        },
        dismissLabel = stringRes(R.string.action__cancel),
        onDismiss = onDismiss,
    ) {
        Column {
            AnimatedVisibility(visible = showAlreadyExistsError) {
                Text(
                    modifier = Modifier.padding(bottom = 16.dp),
                    text = stringRes(R.string.settings__theme_editor__rule_already_exists),
                    color = MaterialTheme.colors.error,
                )
            }

            DialogProperty(text = stringRes(R.string.settings__theme_editor__rule_element)) {
                FlorisDropdownMenu(
                    items = possibleElementLabels,
                    expanded = elementsExpanded,
                    enabled = isAddRuleDialog,
                    selectedIndex = elementsSelectedIndex,
                    isError = showSelectAsError && elementsSelectedIndex == 0,
                    onSelectItem = { elementsSelectedIndex = it },
                    onExpandRequest = { elementsExpanded = true },
                    onDismissRequest = { elementsExpanded = false },
                )
            }

            DialogProperty(text = stringRes(R.string.settings__theme_editor__rule_selectors)) {
                Row(modifier = Modifier.florisHorizontalScroll()) {
                    FlorisChip(
                        onClick = { pressedSelector = !pressedSelector },
                        modifier = Modifier.padding(end = 4.dp),
                        text = when (level) {
                            SnyggLevel.DEVELOPER -> SnyggRule.PRESSED_SELECTOR
                            else -> stringRes(R.string.snygg__rule_selector__pressed)
                        },
                        color = if (pressedSelector) MaterialTheme.colors.primaryVariant else Color.Unspecified,
                    )
                    FlorisChip(
                        onClick = { focusSelector = !focusSelector },
                        modifier = Modifier.padding( end = 4.dp),
                        text = when (level) {
                            SnyggLevel.DEVELOPER -> SnyggRule.FOCUS_SELECTOR
                            else -> stringRes(R.string.snygg__rule_selector__focus)
                        },
                        color = if (focusSelector) MaterialTheme.colors.primaryVariant else Color.Unspecified,
                    )
                    FlorisChip(
                        onClick = { disabledSelector = !disabledSelector },
                        text = when (level) {
                            SnyggLevel.DEVELOPER -> SnyggRule.DISABLED_SELECTOR
                            else -> stringRes(R.string.snygg__rule_selector__disabled)
                        },
                        color = if (disabledSelector) MaterialTheme.colors.primaryVariant else Color.Unspecified,
                    )
                }
            }

            DialogProperty(
                text = stringRes(R.string.settings__theme_editor__rule_codes),
                trailingIconTitle = {
                    FlorisIconButton(
                        onClick = { editCodeDialogValue = NATIVE_NULLPTR },
                        modifier = Modifier.offset(x = 12.dp),
                        icon = painterResource(R.drawable.ic_add),
                    )
                },
            ) {
                if (codes.isEmpty()) {
                    Text(
                        modifier = Modifier.padding(vertical = 4.dp),
                        text = stringRes(R.string.settings__theme_editor__no_codes_defined),
                        fontStyle = FontStyle.Italic,
                    )
                }
                FlowRow {
                    for (code in codes) {
                        FlorisChip(
                            onClick = { editCodeDialogValue = code },
                            text = code.toString(),
                            shape = MaterialTheme.shapes.medium,
                        )
                    }
                }
            }

            DialogProperty(text = stringRes(R.string.settings__theme_editor__rule_modes)) {
                Row(modifier = Modifier.florisHorizontalScroll()) {
                    FlorisChip(
                        onClick = { modeNormal = !modeNormal },
                        modifier = Modifier.padding(end = 4.dp),
                        text = when (level) {
                            SnyggLevel.DEVELOPER -> remember { "m:${InputMode.NORMAL.toString().lowercase()}" }
                            else -> stringRes(R.string.enum__input_mode__normal)
                        },
                        color = if (modeNormal) MaterialTheme.colors.primaryVariant else Color.Unspecified,
                    )
                    FlorisChip(
                        onClick = { modeShiftLock = !modeShiftLock },
                        modifier = Modifier.padding(end = 4.dp),
                        text = when (level) {
                            SnyggLevel.DEVELOPER -> remember { "m:${InputMode.SHIFT_LOCK.toString().lowercase()}" }
                            else -> stringRes(R.string.enum__input_mode__shift_lock)
                        },
                        color = if (modeShiftLock) MaterialTheme.colors.primaryVariant else Color.Unspecified,
                    )
                    FlorisChip(
                        onClick = { modeCapsLock = !modeCapsLock },
                        text = when (level) {
                            SnyggLevel.DEVELOPER -> remember { "m:${InputMode.CAPS_LOCK.toString().lowercase()}" }
                            else -> stringRes(R.string.enum__input_mode__caps_lock)
                        },
                        color = if (modeCapsLock) MaterialTheme.colors.primaryVariant else Color.Unspecified,
                    )
                }
            }
        }
    }

    val initCodeValue = editCodeDialogValue
    if (initCodeValue != null) {
        var inputCodeString by rememberSaveable(initCodeValue) { mutableStateOf(initCodeValue.toString()) }
        var showKeyCodesHelp by rememberSaveable(initCodeValue) { mutableStateOf(false) }
        var showError by rememberSaveable(initCodeValue) { mutableStateOf(false) }
        var errorId by rememberSaveable(initCodeValue) { mutableStateOf<Int>(NATIVE_NULLPTR) }
        JetPrefAlertDialog(
            title = stringRes(if (initCodeValue == NATIVE_NULLPTR) {
                R.string.settings__theme_editor__add_code
            } else {
                R.string.settings__theme_editor__edit_code
            }),
            confirmLabel = stringRes(if (initCodeValue == NATIVE_NULLPTR) {
                R.string.action__add
            } else {
                R.string.action__apply
            }),
            onConfirm = {
                val code = inputCodeString.trim().toIntOrNull(radix = 10)
                when {
                    code == null || (code !in KeyCode.Spec.CHARACTERS && code !in KeyCode.Spec.INTERNAL) -> {
                        errorId = R.string.settings__theme_editor__code_invalid
                        showError = true
                    }
                    code == initCodeValue -> {
                        editCodeDialogValue = null
                    }
                    codes.contains(code) -> {
                        errorId = R.string.settings__theme_editor__code_already_exists
                        showError = true
                    }
                    else -> {
                        codes.add(code)
                        editCodeDialogValue = null
                    }
                }
            },
            dismissLabel = stringRes(R.string.action__cancel),
            onDismiss = {
                editCodeDialogValue = null
            },
            neutralLabel = if (initCodeValue != NATIVE_NULLPTR) {
                stringRes(R.string.action__delete)
            } else {
                null
            },
            onNeutral = {
                codes.remove(initCodeValue)
                editCodeDialogValue = null
            },
            trailingIconTitle = {
                FlorisIconButton(
                    onClick = { showKeyCodesHelp = !showKeyCodesHelp },
                    modifier = Modifier.offset(x = 12.dp),
                    icon = painterResource(R.drawable.ic_help_outline),
                )
            },
        ) {
            Column {
                AnimatedVisibility(visible = showKeyCodesHelp) {
                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        Text(text = stringRes(R.string.settings__theme_editor__code_help_text))
                        FlorisHyperlinkText(
                            text = "Characters (unicode-table.com)",
                            url = stringRes(R.string.florisboard__character_key_codes_url),
                        )
                        FlorisHyperlinkText(
                            text = "Internal (github.com)",
                            url = stringRes(R.string.florisboard__internal_key_codes_url),
                        )
                    }
                }
                OutlinedTextField(
                    value = inputCodeString,
                    onValueChange = { v ->
                        inputCodeString = v
                        showError = false
                    },
                    isError = showError,
                )
                AnimatedVisibility(visible = showError) {
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = stringRes(errorId).curlyFormat(
                            "c_min" to KeyCode.Spec.CHARACTERS_MIN,
                            "c_max" to KeyCode.Spec.CHARACTERS_MAX,
                            "i_min" to KeyCode.Spec.INTERNAL_MIN,
                            "i_max" to KeyCode.Spec.INTERNAL_MAX,
                        ),
                        color = MaterialTheme.colors.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogProperty(
    text: String,
    trailingIconTitle: @Composable () -> Unit = { },
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                text = text,
                style = MaterialTheme.typography.subtitle2,
            )
            trailingIconTitle()
        }
        content()
    }
}

object SnyggValueIcon {
    interface Spec {
        val borderWith: Dp
        val boxShape: Shape
        val elevation: Dp
        val iconSize: Dp
        val iconSizeMinusBorder: Dp
    }

    object Small : Spec {
        override val borderWith = Dp.Hairline
        override val boxShape = RoundedCornerShape(4.dp)
        override val elevation = 4.dp
        override val iconSize = 16.dp
        override val iconSizeMinusBorder = 16.dp
    }

    object Normal : Spec {
        override val borderWith = 1.dp
        override val boxShape = RoundedCornerShape(8.dp)
        override val elevation = 4.dp
        override val iconSize = 24.dp
        override val iconSizeMinusBorder = 22.dp
    }
}

@Composable
private fun SnyggValueIcon(
    value: SnyggValue,
    definedVariables: Map<String, SnyggValue>,
    modifier: Modifier = Modifier,
    spec: SnyggValueIcon.Spec = SnyggValueIcon.Normal,
) {
    when (value) {
        is SnyggSolidColorValue -> {
            Surface(
                modifier = modifier.requiredSize(spec.iconSize),
                color = MaterialTheme.colors.background,
                elevation = spec.elevation,
                shape = spec.boxShape,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(value.color),
                )
            }
        }
        is SnyggShapeValue -> {
            Box(
                modifier = modifier
                    .requiredSize(spec.iconSizeMinusBorder)
                    .border(spec.borderWith, MaterialTheme.colors.onBackground, value.shape)
            )
        }
        is SnyggSpSizeValue -> {
            Icon(
                modifier = modifier.requiredSize(spec.iconSize),
                painter = painterResource(R.drawable.ic_format_size),
                contentDescription = null,
            )
        }
        is SnyggDefinedVarValue -> {
            val realValue = definedVariables[value.key]
            if (realValue == null) {
                Icon(
                    modifier = modifier.requiredSize(spec.iconSize),
                    painter = painterResource(R.drawable.ic_link),
                    contentDescription = null,
                )
            } else {
                val smallSpec = SnyggValueIcon.Small
                Box(modifier = modifier.requiredSize(spec.iconSize)) {
                    SnyggValueIcon(
                        modifier = Modifier.offset(x = 8.dp, y = 8.dp),
                        value = realValue,
                        definedVariables = definedVariables,
                        spec = smallSpec,
                    )
                    Box(
                        modifier = Modifier
                            .offset(x = 1.dp)
                            .requiredSize(smallSpec.iconSize)
                            .padding(vertical = 2.dp)
                            .background(MaterialTheme.colors.background, spec.boxShape),
                    )
                    Icon(
                        modifier = Modifier.requiredSize(smallSpec.iconSize),
                        painter = painterResource(R.drawable.ic_link),
                        contentDescription = null,
                    )
                }
            }
        }
        else -> {
            // Render nothing
        }
    }
}

@Composable
private fun translateElementName(rule: SnyggRule, level: SnyggLevel): String {
    return translateElementName(rule.element, level) ?: remember {
        buildString {
            if (rule.isAnnotation) {
                append(SnyggRule.ANNOTATION_MARKER)
            }
            append(rule.element)
        }
    }
}

@Composable
private fun translateElementName(element: String, level: SnyggLevel): String? {
    return when(level) {
        SnyggLevel.DEVELOPER -> null
        else -> when (element) {
            FlorisImeUi.Keyboard -> R.string.snygg__rule_element__keyboard
            FlorisImeUi.Key -> R.string.snygg__rule_element__key
            FlorisImeUi.KeyHint -> R.string.snygg__rule_element__key_hint
            FlorisImeUi.KeyPopup -> R.string.snygg__rule_element__key_popup
            FlorisImeUi.ClipboardHeader -> R.string.snygg__rule_element__clipboard_header
            FlorisImeUi.ClipboardItem -> R.string.snygg__rule_element__clipboard_item
            FlorisImeUi.ClipboardItemPopup -> R.string.snygg__rule_element__clipboard_item_popup
            FlorisImeUi.OneHandedPanel -> R.string.snygg__rule_element__one_handed_panel
            FlorisImeUi.SmartbarPrimaryRow -> R.string.snygg__rule_element__smartbar_primary_row
            FlorisImeUi.SmartbarPrimaryActionRowToggle -> R.string.snygg__rule_element__smartbar_primary_action_row_toggle
            FlorisImeUi.SmartbarPrimarySecondaryRowToggle -> R.string.snygg__rule_element__smartbar_primary_secondary_row_toggle
            FlorisImeUi.SmartbarSecondaryRow -> R.string.snygg__rule_element__smartbar_secondary_row
            FlorisImeUi.SmartbarActionRow -> R.string.snygg__rule_element__smartbar_action_row
            FlorisImeUi.SmartbarActionButton -> R.string.snygg__rule_element__smartbar_action_button
            FlorisImeUi.SmartbarCandidateRow -> R.string.snygg__rule_element__smartbar_candidate_row
            FlorisImeUi.SmartbarCandidateWord -> R.string.snygg__rule_element__smartbar_candidate_word
            FlorisImeUi.SmartbarCandidateClip -> R.string.snygg__rule_element__smartbar_candidate_clip
            FlorisImeUi.SmartbarCandidateSpacer -> R.string.snygg__rule_element__smartbar_candidate_spacer
            FlorisImeUi.SmartbarKey -> R.string.snygg__rule_element__smartbar_key
            FlorisImeUi.SystemNavBar -> R.string.snygg__rule_element__system_nav_bar
            else -> null
        }
    }.let { if (it != null) { stringRes(it) } else { null } }
}

@Composable
private fun translatePropertyName(propertyName: String, level: SnyggLevel): String {
    return when(level) {
        SnyggLevel.DEVELOPER -> null
        else -> when (propertyName) {
            Snygg.Width -> R.string.snygg__property_name__width
            Snygg.Height -> R.string.snygg__property_name__height
            Snygg.Background -> R.string.snygg__property_name__background
            Snygg.Foreground -> R.string.snygg__property_name__foreground
            Snygg.Border -> R.string.snygg__property_name__border
            Snygg.BorderTop -> R.string.snygg__property_name__border_top
            Snygg.BorderBottom -> R.string.snygg__property_name__border_bottom
            Snygg.BorderStart -> R.string.snygg__property_name__border_start
            Snygg.BorderEnd -> R.string.snygg__property_name__border_end
            Snygg.FontFamily -> R.string.snygg__property_name__font_family
            Snygg.FontSize -> R.string.snygg__property_name__font_size
            Snygg.FontStyle -> R.string.snygg__property_name__font_style
            Snygg.FontVariant -> R.string.snygg__property_name__font_variant
            Snygg.FontWeight -> R.string.snygg__property_name__font_weight
            Snygg.Shadow -> R.string.snygg__property_name__shadow
            Snygg.Shape -> R.string.snygg__property_name__shape
            "--primary" -> R.string.snygg__property_name__var_primary
            "--primary-variant" -> R.string.snygg__property_name__var_primary_variant
            "--secondary" -> R.string.snygg__property_name__var_secondary
            "--secondary-variant" -> R.string.snygg__property_name__var_secondary_variant
            "--background" -> R.string.snygg__property_name__var_background
            "--surface" -> R.string.snygg__property_name__var_surface
            "--surface-variant" -> R.string.snygg__property_name__var_surface_variant
            "--on-primary" -> R.string.snygg__property_name__var_on_primary
            "--on-secondary" -> R.string.snygg__property_name__var_on_secondary
            "--on-background" -> R.string.snygg__property_name__var_on_background
            "--on-surface" -> R.string.snygg__property_name__var_on_surface
            else -> null
        }
    }.let { resId ->
        if (resId != null) {
            stringRes(resId)
        } else {
            propertyName
        }
    }
}

@Composable
private fun translatePropertyValue(propertyValue: SnyggValue, level: SnyggLevel): String {
    return propertyValue.encoder().serialize(propertyValue).getOrElse { propertyValue.toString() }
}
