//
//  CMPAccessibilityContainer.h
//  CMPUIKitUtils
//
//  Created by Ilia.Semenov on 06/12/2023.
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface CMPAccessibilityContainer : UIAccessibilityElement

// need to be redeclared here because of the following issue
// https://youtrack.jetbrains.com/issue/KT-56001/Kotlin-Native-import-Objective-C-category-members-as-class-members-if-the-category-is-located-in-the-same-file

- (__nullable id)accessibilityElementAtIndex:(NSInteger)index;
- (NSInteger)accessibilityElementCount;
- (NSInteger)indexOfAccessibilityElement:(id)element;

@end

NS_ASSUME_NONNULL_END
