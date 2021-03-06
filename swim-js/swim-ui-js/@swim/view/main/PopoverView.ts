// Copyright 2015-2019 SWIM.AI inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

import {Objects} from "@swim/util";
import {PointR2, BoxR2} from "@swim/math";
import {AnyLength, Length} from "@swim/length";
import {Color} from "@swim/color";
import {Ease, Tween, AnyTransition, Transition} from "@swim/transition";
import {MemberAnimator} from "./member/MemberAnimator";
import {View} from "./View";
import {RenderView} from "./RenderView";
import {RenderViewObserver} from "./RenderViewObserver";
import {HtmlView} from "./HtmlView";
import {HtmlViewObserver} from "./HtmlViewObserver";
import {PopoverState, PopoverPlacement, Popover} from "./Popover";
import {PopoverViewObserver} from "./PopoverViewObserver";
import {PopoverViewController} from "./PopoverViewController";

export class PopoverView extends HtmlView implements Popover, HtmlViewObserver, RenderViewObserver {
  /** @hidden */
  _viewController: PopoverViewController | null;
  /** @hidden */
  _source: View | null;
  /** @Hidden */
  _sourceBounds: BoxR2 | null;
  /** @hidden */
  _popoverState: PopoverState;
  /** @hidden */
  readonly _placement: PopoverPlacement[];
  /** @hidden */
  _placementBounds: BoxR2 | null;
  /** @hidden */
  _popoverTransition: Transition<any> | null;
  /** @hidden */

  constructor(node: HTMLElement = document.createElement("div"), key: string | null = null) {
    super(node, key);
    this.arrowWidth.setState(Length.fromAny(10));
    this.arrowHeight.setState(Length.fromAny(8));
    this._source = null;
    this._sourceBounds = null;
    this._popoverState = "shown";
    this._placement = ["top", "bottom", "right", "left", "over"];
    this._placementBounds = null;
    this._popoverTransition = Transition.duration(250, Ease.quadOut);

    const arrow = this.createArrow();
    if (arrow) {
      const arrowView = View.fromNode(arrow).key("arrow");
      this.prependChildView(arrowView);
    }
  }

  protected createArrow(): HTMLElement | null {
    const arrow = document.createElement("div");
    arrow.setAttribute("class", "popover-arrow");
    arrow.style.setProperty("display", "none");
    arrow.style.setProperty("position", "absolute");
    arrow.style.setProperty("width", "0");
    arrow.style.setProperty("height", "0");
    return arrow;
  }

  get viewController(): PopoverViewController | null {
    return this._viewController;
  }

  @MemberAnimator(Length)
  arrowWidth: MemberAnimator<this, Length, AnyLength>;

  @MemberAnimator(Length)
  arrowHeight: MemberAnimator<this, Length, AnyLength>;

  get source(): View | null {
    return this._source;
  }

  setSource(source: View | null): void {
    if (this._source !== source) {
      this.willSetSource(source);
      if (this._source !== null && this.isMounted()) {
        this._source.removeViewObserver(this);
      }
      this._source = source;
      this.onSetSource(source);
      if (this._source !== null && this.isMounted()) {
        this._source.addViewObserver(this);
      }
      this.didSetSource(source);
    }
  }

  protected willSetSource(source: View | null): void {
    this.willObserve(function (viewObserver: PopoverViewObserver): void {
      if (viewObserver.popoverWillSetSource) {
        viewObserver.popoverWillSetSource(source, this);
      }
    });
  }

  protected onSetSource(source: View | null): void {
    // hook
  }

  protected didSetSource(source: View | null): void {
    this.didObserve(function (viewObserver: PopoverViewObserver): void {
      if (viewObserver.popoverDidSetSource) {
        viewObserver.popoverDidSetSource(source, this);
      }
    });
  }

  get popoverState(): PopoverState {
    return this._popoverState;
  }

  get popoverView(): View | null {
    return this;
  }

  togglePopover(tween?: Tween<any>): void {
    if (this._popoverState === "hidden" || this._popoverState === "hiding") {
      this.showPopover(tween);
    } else if (this._popoverState === "shown" || this._popoverState === "showing") {
      this.hidePopover(tween);
    }
  }

  showPopover(tween?: Tween<any>): void {
    if (this._popoverState === "hidden" || this._popoverState === "hiding") {
      if (tween === true) {
        tween = this._popoverTransition || void 0;
      }
      if (tween) {
        tween = Transition.fromAny(tween).onEnd(this.didShow.bind(this));
        this.willShow();
        this.opacity(1, tween);
        this._node.style.setProperty("pointer-events", "auto");
      } else {
        this.willShow();
        this.opacity(1);
        this.didShow();
      }
    }
  }

  protected willShow(): void {
    this._popoverState = "showing";
    this.willObserve(function (viewObserver: PopoverViewObserver): void {
      if (viewObserver.popoverWillShow) {
        viewObserver.popoverWillShow(this);
      }
    });
  }

  protected didShow(): void {
    this._popoverState = "shown";
    this._node.style.setProperty("pointer-events", "auto");
    this.didObserve(function (viewObserver: PopoverViewObserver): void {
      if (viewObserver.popoverDidShow) {
        viewObserver.popoverDidShow(this);
      }
    });
  }

  hidePopover(tween?: Tween<any>): void {
    if (this._popoverState === "shown" || this._popoverState === "showing") {
      if (tween === true) {
        tween = this._popoverTransition || void 0;
      }
      if (tween) {
        tween = Transition.fromAny(tween).onEnd(this.didHide.bind(this));
        this.willHide();
        this.opacity(0, tween);
        this._node.style.setProperty("pointer-events", "none");
      } else {
        this.willHide();
        this.opacity(0);
        this.didHide();
      }
    }
  }

  protected willHide(): void {
    this._popoverState = "hiding";
    this.willObserve(function (viewObserver: PopoverViewObserver): void {
      if (viewObserver.popoverWillHide) {
        viewObserver.popoverWillHide(this);
      }
    });
  }

  protected didHide(): void {
    this._popoverState = "hidden";
    this._node.style.setProperty("pointer-events", "none");
    this.didObserve(function (viewObserver: PopoverViewObserver): void {
      if (viewObserver.popoverDidHide) {
        viewObserver.popoverDidHide(this);
      }
    });
  }

  placement(): ReadonlyArray<PopoverPlacement>;
  placement(placement: ReadonlyArray<PopoverPlacement>): this;
  placement(placement?: ReadonlyArray<PopoverPlacement>): ReadonlyArray<PopoverPlacement> | this {
    if (placement === void 0) {
      return this._placement;
    } else {
      this._placement.length = 0;
      for (let i = 0, n = placement.length; i < n; i += 1) {
        this._placement.push(placement[i]);
      }
      this.setDirty(true);
      return this;
    }
  }

  placementBounds(): BoxR2 | null;
  placementBounds(placementBounds: BoxR2 | null): this;
  placementBounds(placementBounds?: BoxR2 | null): BoxR2 | null | this {
    if (placementBounds === void 0) {
      return this._placementBounds;
    } else {
      if (!Objects.equal(this._placementBounds, placementBounds)) {
        this._placementBounds = placementBounds;
        this.setDirty(true);
      }
      return this;
    }
  }

  popoverTransition(): Transition<any> | null;
  popoverTransition(popoverTransition: AnyTransition<any> | null): this;
  popoverTransition(popoverTransition?: AnyTransition<any> | null): Transition<any> | null | this {
    if (popoverTransition === void 0) {
      return this._popoverTransition;
    } else {
      this._popoverTransition = popoverTransition !== null ? Transition.fromAny(popoverTransition) : null;
      return this;
    }
  }

  protected onMount(): void {
    if (this._source) {
      this._source.addViewObserver(this);
    }
  }

  protected onUnmount(): void {
    if (this._source) {
      this._source.removeViewObserver(this);
    }
  }

  protected onResize(): void {
    this.place();
  }

  protected onScroll(): void {
    this.place();
  }

  protected onAnimate(t: number): void {
    this.arrowWidth.onFrame(t);
    this.arrowHeight.onFrame(t);

    this.place();
  }

  place(): void {
    const source = this._source;
    const oldSourceBounds = this._sourceBounds;
    const newSourceBounds = source ? source.popoverBounds : null;
    if (newSourceBounds && this._placement.length
        && (this._dirty || !newSourceBounds.equals(oldSourceBounds))) {
      this.placePopover(source!, newSourceBounds);
      const arrow = this.getChildView("arrow");
      if (arrow instanceof HtmlView) {
        this.placeArrow(source!, newSourceBounds, arrow);
      }
    }
  }

  /** @hidden */
  protected placePopover(source: View, sourceBounds: BoxR2): void {
    const node = this._node;
    const popoverWidth = node.offsetWidth;
    const popoverHeight = node.offsetHeight;

    // offsetParent bounds in client coordinates
    const nodeBounds = node.getBoundingClientRect();
    const parentLeft = nodeBounds.left - node.offsetLeft;
    const parentTop = nodeBounds.top - node.offsetTop;

    // source bounds in offsetParent coordinates (transformed from page coordinates)
    const sourceLeft = sourceBounds.left - window.pageXOffset + parentLeft;
    const sourceRight = sourceBounds.right - window.pageXOffset + parentLeft;
    const sourceTop = sourceBounds.top - window.pageYOffset + parentTop;
    const sourceBottom = sourceBounds.bottom - window.pageYOffset + parentTop;
    const sourceWidth = sourceBounds.width;
    const sourceHeight = sourceBounds.height;
    const sourceX = sourceLeft + sourceWidth/2;
    const sourceY = sourceTop + sourceHeight/2;

    // placement bounds in offsetParent coordinates (transformed from client coordinates)
    const placementBounds = this._placementBounds;
    const placementLeft = (placementBounds ? placementBounds.left : 0) + parentLeft;
    const placementRight = (placementBounds ? placementBounds.right : window.innerWidth) + parentLeft;
    const placementTop = (placementBounds ? placementBounds.top : 0) + parentTop;
    const placementBottom = (placementBounds ? placementBounds.bottom : window.innerHeight) + parentTop;

    // source bound margins relative to placement bounds
    const marginLeft = placementLeft + sourceLeft;
    const marginRight = document.body.clientWidth - (marginLeft + sourceWidth);
    const marginTop = placementTop + sourceTop;
    const marginBottom = document.body.clientHeight - (marginTop + sourceHeight);

    const arrowHeight = this.arrowHeight.value!.pxValue();

    let placement: PopoverPlacement | undefined;
    for (let i = 0; i < this._placement.length; i += 1) { // first fit
      const p = this._placement[i];
      if (p === "top" && popoverHeight + arrowHeight <= marginTop) {
        placement = "top";
        break;
      } else if (p === "bottom" && popoverHeight + arrowHeight <= marginBottom) {
        placement = "bottom";
        break;
      } else if (p === "left" && popoverWidth + arrowHeight <= marginLeft) {
        placement = "left";
        break;
      } else if (p === "right" && popoverWidth + arrowHeight <= marginRight) {
        placement = "right";
        break;
      }
    }
    if (!placement) {
      for (let i = 0; i < this._placement.length; i += 1) { // best fit
        const p = this._placement[i];
        if (p === "over") {
          placement = "over";
          break;
        } else if (p === "top" && marginTop >= marginBottom) {
          placement = "top";
          break;
        } else if (p === "bottom" && marginBottom >= marginTop) {
          placement = "bottom";
          break;
        } else if (p === "left" && marginLeft >= marginRight) {
          placement = "left";
          break;
        } else if (p === "right" && marginRight >= marginLeft) {
          placement = "right";
          break;
        }
      }
    }

    const oldMaxWidth = Length.fromAny(node.style.getPropertyValue("max-width") || 0).pxValue();
    const oldMaxHeight = Length.fromAny(node.style.getPropertyValue("max-height") || 0).pxValue();
    let maxWidth = oldMaxWidth;
    let maxHeight = oldMaxHeight;
    let left = node.offsetLeft;
    let top = node.offsetTop;
    if (placement === "over") {
      left = placementLeft;
      top = placementTop;
      maxWidth = Math.max(0, placementRight - placementLeft);
      maxHeight = Math.max(0, placementBottom - placementTop);
    } else if (placement === "top") {
      if (sourceX - popoverWidth/2 <= placementLeft) {
        left = placementLeft;
      } else if (sourceX + popoverWidth/2 >= placementRight) {
        left = placementRight - popoverWidth;
      } else {
        left = sourceX - popoverWidth/2;
      }
      top = Math.max(placementTop, sourceTop - (popoverHeight + arrowHeight));
      maxWidth = Math.max(0, placementRight - placementLeft);
      maxHeight = Math.max(0, sourceBottom - placementTop);
    } else if (placement === "bottom") {
      if (sourceX - popoverWidth/2 <= placementLeft) {
        left = placementLeft;
      } else if (sourceX + popoverWidth/2 >= placementRight) {
        left = placementRight - popoverWidth;
      } else {
        left = sourceX - popoverWidth/2;
      }
      top = Math.max(placementTop, sourceBottom + arrowHeight);
      maxWidth = Math.max(0, placementRight - placementLeft);
      maxHeight = Math.max(0, placementBottom - sourceTop);
    } else if (placement === "left") {
      left = Math.max(placementLeft, sourceLeft - (popoverWidth + arrowHeight));
      if (sourceY - popoverHeight/2 <= placementTop) {
        top = placementTop;
      } else if (sourceY + popoverHeight/2 >= placementBottom) {
        top = placementBottom - popoverHeight;
      } else {
        top = sourceY - popoverHeight/2;
      }
      maxWidth = Math.max(0, sourceRight - placementLeft);
      maxHeight = Math.max(0, placementBottom - placementTop);
    } else if (placement === "right") {
      left = Math.max(placementLeft, sourceRight + arrowHeight);
      if (sourceY - popoverHeight/2 <= placementTop) {
        top = placementTop;
      } else if (sourceY + popoverHeight/2 >= placementBottom) {
        top = placementBottom - popoverHeight;
      } else {
        top = sourceY - popoverHeight/2;
      }
      maxWidth = Math.max(0, placementRight - sourceLeft);
      maxHeight = Math.max(0, placementBottom - placementTop);
    }
    if (left !== node.offsetLeft || top !== node.offsetTop
        || maxWidth !== oldMaxWidth || maxHeight !== oldMaxHeight) {
      this.willPlacePopover(placement!);
      node.style.setProperty("position", "absolute");
      node.style.setProperty("left", left + "px");
      node.style.setProperty("top", top + "px");
      node.style.setProperty("max-width", maxWidth + "px");
      node.style.setProperty("max-height", maxHeight + "px");
      this.onPlacePopover(placement!);
      this.didPlacePopover(placement!);
    }
  }

  protected willPlacePopover(placement: PopoverPlacement): void {
    this.willObserve(function (viewObserver: PopoverViewObserver): void {
      if (viewObserver.popoverWillPlace) {
        viewObserver.popoverWillPlace(placement, this);
      }
    });
  }

  protected onPlacePopover(placement: PopoverPlacement): void {
    // hook
  }

  protected didPlacePopover(placement: PopoverPlacement): void {
    this.didObserve(function (viewObserver: PopoverViewObserver): void {
      if (viewObserver.popoverDidPlace) {
        viewObserver.popoverDidPlace(placement, this);
      }
    });
  }

  /** @hidden */
  protected placeArrow(source: View, sourceBounds: BoxR2, arrow: HtmlView): void {
    const node = this._node;
    const arrowNode = arrow._node;

    // offsetParent bounds in client coordinates
    const nodeBounds = node.getBoundingClientRect();
    const parentLeft = nodeBounds.left - node.offsetLeft;
    const parentTop = nodeBounds.top - node.offsetTop;

    // source bounds in offsetParent coordinates (transformed from page coordinates)
    const sourceLeft = sourceBounds.left - window.pageXOffset + parentLeft;
    const sourceTop = sourceBounds.top - window.pageYOffset + parentTop;
    const sourceWidth = sourceBounds.width;
    const sourceHeight = sourceBounds.height;
    const sourceX = sourceLeft + sourceWidth/2;
    const sourceY = sourceTop + sourceHeight/2;

    const offsetLeft = node.offsetLeft;
    const offsetRight = offsetLeft + node.offsetWidth;
    const offsetTop = node.offsetTop;
    const offsetBottom = offsetTop + node.offsetHeight;

    const backgroundColor = this.backgroundColor() || Color.transparent();
    const borderRadius = this.borderRadius();
    const radius = borderRadius instanceof Length ? borderRadius.pxValue() : 0;

    const arrowWidth = this.arrowWidth.value!.pxValue();
    const arrowHeight = this.arrowHeight.value!.pxValue();

    const arrowXMin = offsetLeft + radius + arrowWidth/2;
    const arrowXMax = offsetRight - radius - arrowWidth/2;
    const arrowYMin = offsetTop + radius + arrowWidth/2;
    const arrowYMax = offsetBottom - radius - arrowWidth/2;

    arrowNode.style.removeProperty("top");
    arrowNode.style.removeProperty("right");
    arrowNode.style.removeProperty("bottom");
    arrowNode.style.removeProperty("left");
    arrowNode.style.removeProperty("border-left-width");
    arrowNode.style.removeProperty("border-left-style");
    arrowNode.style.removeProperty("border-left-color");
    arrowNode.style.removeProperty("border-right-width");
    arrowNode.style.removeProperty("border-right-style");
    arrowNode.style.removeProperty("border-right-color");
    arrowNode.style.removeProperty("border-top-width");
    arrowNode.style.removeProperty("border-top-style");
    arrowNode.style.removeProperty("border-top-color");
    arrowNode.style.removeProperty("border-bottom-width");
    arrowNode.style.removeProperty("border-bottom-style");
    arrowNode.style.removeProperty("border-bottom-color");

    if (offsetTop - arrowHeight >= sourceY // arrow tip below source center
        && arrowXMin <= sourceX && sourceX <= arrowXMax) { // arrow base on top popover edge
      // top arrow
      arrowNode.style.setProperty("display", "block");
      arrowNode.style.setProperty("top", (-arrowHeight) + "px");
      arrowNode.style.setProperty("left", (sourceX - offsetLeft - arrowWidth/2) + "px");
      arrowNode.style.setProperty("border-left-width", (arrowWidth/2) + "px");
      arrowNode.style.setProperty("border-left-style", "solid");
      arrowNode.style.setProperty("border-left-color", "transparent");
      arrowNode.style.setProperty("border-right-width", (arrowWidth/2) + "px");
      arrowNode.style.setProperty("border-right-style", "solid");
      arrowNode.style.setProperty("border-right-color", "transparent");
      arrowNode.style.setProperty("border-bottom-width", arrowHeight + "px");
      arrowNode.style.setProperty("border-bottom-style", "solid");
      arrowNode.style.setProperty("border-bottom-color", backgroundColor.toString());
    } else if (offsetBottom + arrowHeight <= sourceY // arrow tip above source center
        && arrowXMin <= sourceX && sourceX <= arrowXMax) { // arrow base on bottom popover edge
      // bottom arrow
      arrowNode.style.setProperty("display", "block");
      arrowNode.style.setProperty("bottom", (-arrowHeight) + "px");
      arrowNode.style.setProperty("left", (sourceX - offsetLeft - arrowWidth/2) + "px");
      arrowNode.style.setProperty("border-left-width", (arrowWidth/2) + "px");
      arrowNode.style.setProperty("border-left-style", "solid");
      arrowNode.style.setProperty("border-left-color", "transparent");
      arrowNode.style.setProperty("border-right-width", (arrowWidth/2) + "px");
      arrowNode.style.setProperty("border-right-style", "solid");
      arrowNode.style.setProperty("border-right-color", "transparent");
      arrowNode.style.setProperty("border-top-width", arrowHeight + "px");
      arrowNode.style.setProperty("border-top-style", "solid");
      arrowNode.style.setProperty("border-top-color", backgroundColor.toString());
    } else if (offsetLeft - arrowHeight >= sourceX // arrow tip right of source center
        && arrowYMin <= sourceY && sourceY <= arrowYMax) { // arrow base on left popover edge
      // left arrow
      arrowNode.style.setProperty("display", "block");
      arrowNode.style.setProperty("left", (-arrowHeight) + "px");
      arrowNode.style.setProperty("top", (sourceY - offsetTop - arrowWidth/2) + "px");
      arrowNode.style.setProperty("border-top-width", (arrowWidth/2) + "px");
      arrowNode.style.setProperty("border-top-style", "solid");
      arrowNode.style.setProperty("border-top-color", "transparent");
      arrowNode.style.setProperty("border-bottom-width", (arrowWidth/2) + "px");
      arrowNode.style.setProperty("border-bottom-style", "solid");
      arrowNode.style.setProperty("border-bottom-color", "transparent");
      arrowNode.style.setProperty("border-right-width", arrowHeight + "px");
      arrowNode.style.setProperty("border-right-style", "solid");
      arrowNode.style.setProperty("border-right-color", backgroundColor.toString());
    } else if (offsetRight + arrowHeight <= sourceX // arrow tip left of source center
        && arrowYMin <= sourceY && sourceY <= arrowYMax) { // arrow base on right popover edge
      // right arrow
      arrowNode.style.setProperty("display", "block");
      arrowNode.style.setProperty("right", (-arrowHeight) + "px");
      arrowNode.style.setProperty("top", (sourceY - offsetTop - arrowWidth/2) + "px");
      arrowNode.style.setProperty("border-top-width", (arrowWidth/2) + "px");
      arrowNode.style.setProperty("border-top-style", "solid");
      arrowNode.style.setProperty("border-top-color", "transparent");
      arrowNode.style.setProperty("border-bottom-width", (arrowWidth/2) + "px");
      arrowNode.style.setProperty("border-bottom-style", "solid");
      arrowNode.style.setProperty("border-bottom-color", "transparent");
      arrowNode.style.setProperty("border-left-width", arrowHeight + "px");
      arrowNode.style.setProperty("border-left-style", "solid");
      arrowNode.style.setProperty("border-left-color", backgroundColor.toString());
    } else {
      // no arrow
      arrowNode.style.setProperty("display", "none");
    }
  }

  viewDidMount(view: View): void {
    this.place();
  }

  viewDidUnmount(view: View): void {
    this.place();
  }

  viewDidResize(view: View): void {
    this.place();
  }

  viewDidScroll(view: View): void {
    this.place();
  }

  viewWillSetAttribute(name: string, value: unknown, view: HtmlView): void {
    this.place();
  }

  viewDidSetAttribute(name: string, value: unknown, view: HtmlView): void {
    this.place();
  }

  viewWillSetStyle(name: string, value: unknown, priority: string | undefined, view: HtmlView): void {
    this.place();
  }

  viewDidSetStyle(name: string, value: unknown, priority: string | undefined, view: HtmlView): void {
    this.place();
  }

  viewDidSetAnchor(newAnchor: PointR2, oldAnchor: PointR2, view: RenderView): void {
    this.place();
  }
}
