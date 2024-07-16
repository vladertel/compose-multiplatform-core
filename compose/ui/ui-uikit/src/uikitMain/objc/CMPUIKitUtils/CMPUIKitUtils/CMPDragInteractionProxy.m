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


#import "CMPDragInteractionProxy.h"

@implementation CMPDragInteractionProxy

+ (NSItemProvider *)itemProviderFromString:(NSString *)string {
    return [[NSItemProvider alloc] initWithObject:string];
}

- (nonnull NSArray<UIDragItem *> *)dragInteraction:(nonnull UIDragInteraction *)interaction itemsForBeginningSession:(nonnull id<UIDragSession>)session {
    return [self itemsForBeginningDragSession:session interaction:interaction];
}

- (nonnull NSArray<UIDragItem *> *)itemsForBeginningDragSession:(nonnull id<UIDragSession>)session interaction:(nonnull UIDragInteraction *)interaction {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (BOOL)dragInteraction:(UIDragInteraction *)interaction sessionAllowsMoveOperation:(id<UIDragSession>)session {
    return [self dragSessionAllowsMoveOperation:session interaction:interaction];
}

- (BOOL)dragSessionAllowsMoveOperation:(nonnull id<UIDragSession>)session interaction:(nonnull UIDragInteraction *)interaction {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (BOOL)dragInteraction:(UIDragInteraction *)interaction sessionIsRestrictedToDraggingApplication:(id<UIDragSession>)session {
    return [self dragSessionIsRestrictedToDraggingApplication:session interaction:interaction];
}

- (BOOL)dragSessionIsRestrictedToDraggingApplication:(nonnull id<UIDragSession>)session interaction:(nonnull UIDragInteraction *)interaction {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

@end

@implementation UIDragItem (CMPInitializers)

+ (instancetype)itemWithString:(NSString *)string {
    return [[UIDragItem alloc] initWithItemProvider:[[NSItemProvider alloc] initWithObject:string]];
}

@end
