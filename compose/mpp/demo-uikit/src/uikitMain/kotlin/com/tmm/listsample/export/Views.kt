package com.tmm.listsample.export

import androidx.compose.ui.window.Application
import com.tmm.listsample.components.MainUiNoImageUseModel
import platform.UIKit.UIViewController

/**
 * export to oc
 * @return UIViewController
 */
fun createDisplayViewUsingComposeJBContainer(): UIViewController {
    return Application {
        MainUiNoImageUseModel()
    }
}