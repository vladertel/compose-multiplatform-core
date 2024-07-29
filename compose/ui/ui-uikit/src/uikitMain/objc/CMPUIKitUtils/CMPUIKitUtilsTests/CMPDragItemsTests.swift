/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import XCTest
import Foundation

final class StringWrapper: NSObject, NSItemProviderWriting, NSItemProviderReading {
    static var writableTypeIdentifiersForItemProvider: [String] {
        ["org.jetbrains.custom-string"]
    }
    
    static var readableTypeIdentifiersForItemProvider: [String] {
        writableTypeIdentifiersForItemProvider
    }
    
    let string: String
    
    init(string: String) {
        self.string = string
    }
    
    func loadData(withTypeIdentifier typeIdentifier: String, forItemProviderCompletionHandler completionHandler: @escaping @Sendable (Data?, (any Error)?) -> Void) -> Progress? {
        DispatchQueue.global().asyncAfter(deadline: .now() + 0.5) {
            completionHandler(self.string.data(using: .utf8)!, nil)
        }
        
        return nil
    }
    
    static func object(withItemProviderData data: Data, typeIdentifier: String) throws -> Self {
        return Self(string: String(data: data, encoding: .utf8)!)
    }
    
    override func isEqual(_ object: Any?) -> Bool {
        guard let object = object as? StringWrapper else {
            return false
        }
        
        return object.string == string
    }
}

final class CMPDragItemsTests: XCTestCase {
    @MainActor
    func testDragItems() async throws {
        let sourceString = "Sample string"
                
        let stringDragItem = UIDragItem.cmp_item(with: sourceString)
        
        let decodedString = try await stringDragItem.cmp_loadString()
        XCTAssertEqual(sourceString, decodedString)
        
        let decodedNSString = try await stringDragItem.cmp_loadObject(of: NSString.self) as! NSString
        XCTAssertEqual(NSString(string: sourceString), decodedNSString)
                
        let sourceStringWrapper = StringWrapper(string: sourceString)
        
        guard let stringWrapperDragItem = UIDragItem.cmp_item(with: sourceStringWrapper, of: StringWrapper.self) else {
            XCTFail()
            return
        }
        
        guard let decodedStringWrapper = try await stringWrapperDragItem.cmp_loadObject(of: StringWrapper.self) as? StringWrapper else {
            XCTFail()
            return
        }
        
        guard let anotherDecodedStringWrapper = try await stringWrapperDragItem.cmp_loadObject(of: StringWrapper.self) as? StringWrapper else {
            XCTFail()
            return
        }
        
        XCTAssertEqual(decodedStringWrapper, anotherDecodedStringWrapper)
        
        XCTAssertEqual(sourceString, anotherDecodedStringWrapper.string)
    }
}
