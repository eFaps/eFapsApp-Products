/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
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
package org.efaps.esjp.products.util;

import org.efaps.admin.datamodel.IBitEnum;
import org.efaps.admin.datamodel.attributetype.BitEnumType;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;

@EFapsUUID("994eae47-2596-4da7-9247-ac8a058e4ecc")
@EFapsApplication("eFapsApp-Products")
public enum ConversionType implements IBitEnum
{

    TRANSPORTWEIGHT;

    @Override
    public int getInt()
    {
        return BitEnumType.getInt4Index(ordinal());
    }

    @Override
    public int getBitIndex()
    {
        return ordinal();
    }
}
