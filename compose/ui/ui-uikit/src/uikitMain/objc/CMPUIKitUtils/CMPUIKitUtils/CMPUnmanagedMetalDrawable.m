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

#import "CMPUnmanagedMetalDrawable.h"

CMPUnmanagedMetalDrawable CMPUnmanagedMetalDrawableNextFromLayer(CAMetalLayer *metalLayer) {
    return (__bridge_retained void *)[metalLayer nextDrawable];
}

void CMPUnmanagedMetalDrawableRelease(CMPUnmanagedMetalDrawable unmanagedDrawable) {
    /// `drawable` will be released by ARC
    __attribute__((unused)) id <CAMetalDrawable> drawable = (__bridge_transfer id <CAMetalDrawable>)unmanagedDrawable;
}

void *CMPUnmanagedMetalDrawableGetTexture(CMPUnmanagedMetalDrawable unmanagedDrawable) {
    id <CAMetalDrawable> drawable = (__bridge id <CAMetalDrawable>)unmanagedDrawable;
    
    return (__bridge_retained void *)[drawable texture];
}
