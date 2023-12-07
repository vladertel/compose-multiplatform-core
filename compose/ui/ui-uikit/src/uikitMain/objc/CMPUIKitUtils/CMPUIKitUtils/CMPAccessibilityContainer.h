//
//  CMPAccessibilityContainer.h
//  CMPUIKitUtils
//
//  Created by Ilia.Semenov on 06/12/2023.
//

#import <UIKit/UIKit.h>
#import "CMPAccessibilityElement.h"

NS_ASSUME_NONNULL_BEGIN

@interface CMPAccessibilityContainer : CMPAccessibilityElement

-(nullable id)accessibilityElementAtIndex:(NSInteger)index;
-(NSInteger)accessibilityElementCount;
-(NSInteger)indexOfAccessibilityElement:(id)element;

@end

NS_ASSUME_NONNULL_END
