/*
 * Copyright 2003 - 2023 The eFaps Team
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
 *
 */
package org.efaps.esjp.products.eql;

import java.util.List;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.eql.AbstractSelect;
import org.efaps.util.EFapsException;

@EFapsUUID("016b975c-43c4-4bc9-86d5-1ad9aa8323f0")
@EFapsApplication("eFapsApp-Products")
public abstract class BarcodeSelect_Base
    extends AbstractSelect
{

    @Override
    public void initialize(List<Instance> instances,
                           String... typeArg)
        throws EFapsException
    {
        final var barcodeTypeID = Long.valueOf(typeArg[0]);
        final var eval = EQL.builder().print(instances.toArray(new Instance[instances.size()]))
                        .attributeSet(CIProducts.ProductAbstract.Barcodes)
                        .attribute("Code").as("Code")
                        .attributeSet(CIProducts.ProductAbstract.Barcodes)
                        .attribute("BarcodeType").as("Type")
                        .evaluate();
        while (eval.next()) {
            final var productInstance = eval.inst();
            final var codes = eval.<List<String>>get("Code");
            final var typeIds = eval.<List<Long>>get("Type");
            if (codes != null) {
                final var codeIter = codes.iterator();
                for (final Long typeId : typeIds) {
                    final var code = codeIter.next();
                    if (typeId != null && typeId.equals(barcodeTypeID)) {
                        getValues().put(productInstance, code);
                        break;
                    }
                }
            }
        }
    }
}
