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
package org.efaps.esjp.products.eql;

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.lang3.EnumUtils;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.eql.AbstractSelect;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.products.Conversion;
import org.efaps.esjp.products.util.ConversionType;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("3afd716b-81ed-4ef6-b1ef-eb9956c833d8")
@EFapsApplication("eFapsApp-Products")
public abstract class ConversionSelect_Base
    extends AbstractSelect
{
    private static final Logger LOG = LoggerFactory.getLogger(ConversionSelect.class);

    @Override
    public void initialize(final List<Instance> _instances, final String... _arguments)
        throws EFapsException
    {
        if (_arguments.length < 1) {
            LOG.warn("Missing oblicatory first argument 'ConversionType' ");
        } else {
            if (!_instances.isEmpty()) {
                final var conversionType = EnumUtils.getEnum(ConversionType.class, _arguments[0]);

                if (InstanceUtils.isKindOf(_instances.get(0), CIProducts.ProductAbstract)) {
                    // TODO
                } else {
                    String productSelect = null;
                    if (_arguments.length < 2) {
                        // TODO
                    } else {
                        productSelect =  _arguments[1];
                    }
                    final var quantitySelect =  _arguments[2];
                    final var uomSelect = _arguments[3];

                    final var eval = EQL.builder()
                                    .print(_instances.toArray(new Instance[_instances.size()]))
                                        .select(productSelect).as("prodSel")
                                        .select(quantitySelect).as("qtySel")
                                        .select(uomSelect).as("uomSel")
                                    .evaluate();
                    while (eval.next()) {
                        final var prodInst = (Instance) eval.get("prodSel");
                        final var qty  = (BigDecimal) eval.get("qtySel");
                        final var uomObj  = eval.get("uomSel");
                        UoM uom = null;
                        if (uomObj instanceof Long) {
                            uom = Dimension.getUoM((Long) uomObj);
                        }
                        final var convValue = Conversion.convert(conversionType, prodInst, qty, uom);
                        if (convValue != null) {
                            getValues().put(eval.inst(),
                                        new Object[] { convValue.getValue(), convValue.getUoM().getSymbol(),
                                                        convValue.getUoM().getName(),
                                                        convValue.getUoM().getCommonCode() });
                        }
                    }
                }
            }
        }
    }
}
