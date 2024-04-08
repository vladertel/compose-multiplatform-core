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

#import "CMPDebugBadge.h"

@implementation CMPDebugBadge

- (instancetype)initWithFrame:(CGRect)frame {
    self = [super initWithFrame:frame];
    
    if (self) {
        self.translatesAutoresizingMaskIntoConstraints = false;
        self.opaque = false;
        
        [NSLayoutConstraint activateConstraints:@[
            [self.widthAnchor constraintEqualToConstant:150.0],
            [self.heightAnchor constraintEqualToConstant:30.0]
        ]];
    }
    
    return self;
}

- (void)drawRect:(CGRect)rect {
    CGContextRef context = UIGraphicsGetCurrentContext();
    
    CGContextSetFillColorWithColor(context, [UIColor systemRedColor].CGColor);

    CGPoint p0 = CGPointMake(0.0, 0.0);
    CGPoint p1 = CGPointMake(rect.size.width, rect.size.height);
    CGPoint p2 = CGPointMake(rect.size.width, 0.0);
    
    CGPoint points[] = { p0, p1, p2 };
    
    CGContextAddLines(context, points, 3);
    CGContextFillPath(context);

    CGContextSaveGState(context);
    
    CGFloat angle = atan2(rect.size.height, rect.size.width);
    CGContextRotateCTM(context, angle);

    NSDictionary *textAttributes = @{
        NSFontAttributeName: [UIFont boldSystemFontOfSize:12],
        NSForegroundColorAttributeName: [UIColor whiteColor]
    };
    NSString *text = @"DEBUG";

    CGSize textSize = [text sizeWithAttributes:textAttributes];
    CGFloat diagonal = sqrt(rect.size.width * rect.size.width + rect.size.height * rect.size.height);
    CGPoint textOrigin = CGPointMake(diagonal / 2.0 - textSize.width / 2 + 10.0, -textSize.height);

    [text drawAtPoint:textOrigin withAttributes:textAttributes];

    CGContextRestoreGState(context);
}

- (void)layoutSubviews {
    [super layoutSubviews];
    
    [self setNeedsDisplay];
}

@end
