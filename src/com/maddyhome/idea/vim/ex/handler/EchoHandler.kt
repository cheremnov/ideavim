/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.ex.handler

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.maddyhome.idea.vim.ex.CommandHandler
import com.maddyhome.idea.vim.ex.ExOutputModel
import com.maddyhome.idea.vim.ex.flags
import dev.feedforward.vim.lang.psi.VimEchoCommand
import dev.feedforward.vim.lang.psi.VimLiteralExpr

/**
 * @author vlan
 */
class EchoHandler : CommandHandler.PsiExecution() {
  override val argFlags = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun execute(editor: Editor, context: DataContext, cmd: PsiElement): Boolean {
    val vimCommand = cmd as? VimEchoCommand ?: return false

    val expressions = vimCommand.exprList
    val text = expressions.joinToString(" ", postfix = "\n") {
      (it as? VimLiteralExpr)?.stringLiteral?.text ?: "ERROR"
    }
    ExOutputModel.getInstance(editor).output(text)
    return true
  }
}

