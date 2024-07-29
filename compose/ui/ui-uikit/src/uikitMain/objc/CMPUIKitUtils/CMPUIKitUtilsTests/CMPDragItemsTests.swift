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

enum BinaryDataSerializationError: Error {
    case unsupportedUti
}

final class BinaryData: NSObject, NSItemProviderWriting, NSItemProviderReading {
    static var writableTypeIdentifiersForItemProvider: [String] {
        ["org.jetbrains.bin"]
    }
    
    static var readableTypeIdentifiersForItemProvider: [String] {
        writableTypeIdentifiersForItemProvider
    }
    
    let data: Data
    
    init(data: Data) {
        self.data = data
    }
    
    func loadData(withTypeIdentifier typeIdentifier: String, forItemProviderCompletionHandler completionHandler: @escaping @Sendable (Data?, (any Error)?) -> Void) -> Progress? {
        DispatchQueue.global().asyncAfter(deadline: .now() + 0.5) {
            completionHandler(self.data, nil)
        }
        
        return nil
    }
    
    static func object(withItemProviderData data: Data, typeIdentifier: String) throws -> Self {
        return Self(data: data)
    }
    
    override func isEqual(_ object: Any?) -> Bool {
        guard let object = object as? BinaryData else {
            return false
        }
        
        return object.data == data
    }
}

final class CMPDragItemsTests: XCTestCase {
    func testDragItems() async throws {
        let sourceString = "Sample string"
                
        let stringDragItem = await UIDragItem.cmp_item(with: sourceString)
        
        let decodedString = try await stringDragItem.cmp_loadString()
        XCTAssertEqual(sourceString, decodedString)
        
        let decodedNSString = try await stringDragItem.cmp_loadAny(NSString.self) as! NSString
        XCTAssertEqual(NSString(string: sourceString), decodedNSString)
                
        guard let sourceStringData = sourceString.data(using: .utf8) else {
            XCTFail()
            return
        }
        
        let sourceBinaryData = BinaryData(data: sourceStringData)
        
        guard let binaryDataDragItem = await UIDragItem.cmp_item(withAny: BinaryData.self, object: sourceBinaryData) else {
            XCTFail()
            return
        }
        
        guard let decodedBinaryData = try await binaryDataDragItem.cmp_loadAny(BinaryData.self) as? BinaryData else {
            XCTFail()
            return
        }
        
        guard let anotherDecodedBinaryData = try await binaryDataDragItem.cmp_loadAny(BinaryData.self) as? BinaryData else {
            XCTFail()
            return
        }
        
        XCTAssertEqual(decodedBinaryData, anotherDecodedBinaryData)
        
        guard let decodedBinaryDataString = String(data: anotherDecodedBinaryData.data, encoding: .utf8) else {
            XCTFail()
            return
        }
        
        XCTAssertEqual(sourceString, decodedBinaryDataString)
    }
}
