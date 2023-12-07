//
//  CMPAccessibilityContainer.h
//  CMPUIKitUtils
//
//  Created by Ilia.Semenov on 06/12/2023.
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface CMPAccessibilityContainer : UIAccessibilityElement

@property(nonatomic) NSMutableArray* children;
@property(nonatomic, nullable) id wrappedElement;

-(nullable id)accessibilityElementAtIndex:(NSInteger)index;
-(NSInteger)accessibilityElementCount;
-(NSInteger)indexOfAccessibilityElement:(id)element;

@end

NS_ASSUME_NONNULL_END
