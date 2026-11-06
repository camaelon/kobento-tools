#import <UIKit/UIKit.h>
#import <MetalKit/MetalKit.h>

NS_ASSUME_NONNULL_BEGIN

/// One RemoteCompose document played with the C++ engine and Skia on Metal, laid out to the view.
///
/// The document is laid out at the view's size in points (dp, as the web and C++ players read a
/// document) and drawn at the screen's scale, so it fills the view and lays out again when the
/// view changes size (a rotation, a split view). Touches are in points, the same coordinates.
@interface KBPlayer : NSObject

- (nullable instancetype)initWithDevice:(id<MTLDevice>)device commandQueue:(id<MTLCommandQueue>)queue;

/// Parses the .rc bytes. NO when they are not a document the engine can read.
- (BOOL)loadData:(NSData *)data;

/// The theme the document is given: dark or light. Changing it restarts the document in it.
@property (nonatomic) BOOL dark;

/// The colour behind the document (ARGB), where it draws nothing.
@property (nonatomic) uint32_t backgroundArgb;

/// The size the document says it is designed at (dp), or zero.
@property (nonatomic, readonly) CGSize designSize;

/// Paints one frame into the view's drawable and presents it.
- (void)drawInView:(MTKView *)view animTime:(double)animTime deltaTime:(double)deltaTime;

/// Whether the document wants another frame: an animation, a clock, a fling coasting.
- (BOOL)needsAnimation;

- (void)touchDownX:(float)x y:(float)y;
- (void)touchDragX:(float)x y:(float)y;
- (void)touchUpX:(float)x y:(float)y velocityX:(float)vx velocityY:(float)vy;
/// A tap: the document's click areas (a button's onClick). Send it after touchUp.
- (void)clickX:(float)x y:(float)y;

@end

NS_ASSUME_NONNULL_END
