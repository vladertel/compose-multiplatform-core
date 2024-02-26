//
//  CMPUIKitUtilsTestAppApp.swift
//  CMPUIKitUtilsTestApp
//
//  Created by Andrei.Salavei on 30.11.23.
//

import SwiftUI

@main
struct CMPUIKitUtilsTestApp: App {   
    let uiLogger = CMPOSLogger(categoryName: "androidx.compose.ui")
    let runtimeLogger = CMPOSLogger(categoryName: "androidx.compose.runtime")
    var body: some Scene {
        WindowGroup {
            Color.black
                .onAppear {
                    Task {
                        if #available(iOS 16.0, *) {
                            for i in 0..<100 {
                                let uiInterval = uiLogger.beginIntervalNamed("\(i)")
                                let runtimeInterval = runtimeLogger.beginIntervalNamed("\(i)")
                                try await Task.sleep(for: Duration.milliseconds(4))
                                uiLogger.end(uiInterval)
                                runtimeLogger.end(runtimeInterval)
                                try await Task.sleep(for: Duration.milliseconds(4))
                            }
                        }
                    }
                }
        }
    }
}
