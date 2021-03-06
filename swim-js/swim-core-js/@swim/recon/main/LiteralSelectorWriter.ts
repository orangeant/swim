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

import {Output, WriterException, Writer} from "@swim/codec";
import {ReconWriter} from "./ReconWriter";

/** @hidden */
export class LiteralSelectorWriter<I, V> extends Writer {
  private readonly _recon: ReconWriter<I, V>;
  private readonly _item: I;
  private readonly _then: V;
  private readonly _part: Writer | undefined;
  private readonly _step: number | undefined;

  constructor(recon: ReconWriter<I, V>, item: I, then: V, part?: Writer, step?: number) {
    super();
    this._recon = recon;
    this._item = item;
    this._then = then;
    this._part = part;
    this._step = step;
  }

  pull(output: Output): Writer {
    return LiteralSelectorWriter.write(output, this._recon, this._item, this._then,
                                       this._part, this._step);
  }

  static sizeOf<I, V>(recon: ReconWriter<I, V>, item: I, then: V): number {
    let size = 0;
    if (recon.precedence(item) < recon.precedence(recon.item(then))) {
      size += 1; // '('
      size += recon.sizeOfItem(item);
      size += 1; // ')'
    } else {
      size += recon.sizeOfItem(item);
    }
    size += recon.sizeOfThen(then);
    return size;
  }

  static write<I, V>(output: Output, recon: ReconWriter<I, V>, item: I, then: V,
                     part?: Writer, step: number = 1): Writer {
    if (step === 1) {
      if (recon.precedence(item) < recon.precedence(recon.item(then))) {
        if (output.isCont()) {
          output = output.write(40/*'('*/);
          step = 2;
        }
      } else {
        step = 2;
      }
    }
    if (step === 2) {
      if (!part) {
        part = recon.writeItem(item, output);
      } else {
        part = part.pull(output);
      }
      if (part.isDone()) {
        part = void 0;
        step = 3;
      } else if (part.isError()) {
        return part.asError();
      }
    }
    if (step === 3) {
      if (recon.precedence(item) < recon.precedence(recon.item(then))) {
        if (output.isCont()) {
          output = output.write(41/*')'*/);
          step = 4;
        }
      } else {
        step = 4;
      }
    }
    if (step === 4) {
      return recon.writeThen(then, output);
    }
    if (output.isDone()) {
      return Writer.error(new WriterException("truncated"));
    } else if (output.isError()) {
      return Writer.error(output.trap());
    }
    return new LiteralSelectorWriter<I, V>(recon, item, then, part, step);
  }
}
