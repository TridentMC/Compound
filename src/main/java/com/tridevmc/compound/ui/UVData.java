/*
 * Copyright 2018 - 2022 TridentMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tridevmc.compound.ui;

import com.google.common.base.MoreObjects;

/**
 * Used for defining UV data of rects to draw on the screen.
 */
public class UVData {

    private final float u, v;

    public UVData(float u, float v) {
        this.u = u;
        this.v = v;
    }

    public float getU() {
        return this.u;
    }

    public float getV() {
        return this.v;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("u", this.u)
                .add("v", this.v)
                .toString();
    }

}
