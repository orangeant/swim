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

import {Murmur3} from "@swim/util";
import {Output} from "@swim/codec";
import {LengthUnits, Length} from "./Length";

export class PctLength extends Length {
  /** @hidden */
  readonly _value: number;
  /** @hidden */
  readonly _node: Node | undefined;

  constructor(value: number, node?: Node) {
    super();
    this._value = value;
    this._node = node;
  }

  value(): number {
    return this._value;
  }

  units(): LengthUnits {
    return "%";
  }

  node(): Node | undefined {
    return this._node;
  }

  unitValue(): number {
    return Length.widthUnit(this._node);
  }

  pxValue(unitValue: number = this.unitValue()): number {
    return unitValue * this._value / 100;
  }

  pct(): PctLength {
    return this;
  }

  equals(that: unknown): boolean {
    if (that instanceof PctLength) {
      return this._value === that._value && this._node === that._node;
    }
    return false;
  }

  hashCode(): number {
    if (PctLength._hashSeed === void 0) {
      PctLength._hashSeed = Murmur3.seed(PctLength);
    }
    return Murmur3.mash(Murmur3.mix(PctLength._hashSeed, Murmur3.hash(this._value)));
  }

  debug(output: Output): void {
    output = output.write("Length").write(46/*'.'*/).write("pct").write(40/*'('*/).debug(this._value);
    if (this._node) {
      output = output.write(", ").debug(this._node);
    }
    output = output.write(41/*')'*/);
  }

  toString(): string {
    return this._value + "%";
  }

  private static _hashSeed?: number;
}
Length.Pct = PctLength;
