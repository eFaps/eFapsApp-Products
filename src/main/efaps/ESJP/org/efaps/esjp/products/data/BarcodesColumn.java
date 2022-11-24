/*
 * Copyright 2003 - 2022 The eFaps Team
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
package org.efaps.esjp.products.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.efaps.admin.datamodel.AttributeSet;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.data.IColumnValidate;
import org.efaps.esjp.data.IOnRow;
import org.efaps.esjp.data.jaxb.AbstractDef;
import org.efaps.esjp.data.jaxb.AttrDef;
import org.efaps.esjp.data.jaxb.AttrSetDef;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("6fac24cb-e154-4952-a4f8-69ec638dd709")
@EFapsApplication("eFapsApp-Products")
public class BarcodesColumn
    extends AbstractCommon
    implements IColumnValidate, IOnRow
{

    private static final Logger LOG = LoggerFactory.getLogger(BarcodesColumn.class);

    @Override
    public Collection<String> getColumnNames(final Parameter _parameter, final AbstractDef _def)
    {
        final List<String> ret = new ArrayList<>();
        final String column = ((AttrDef) _def).getProperty("Column");
        ret.add(column);
        return ret;
    }

    @Override
    public Boolean validate(final Parameter _parameter, final AbstractDef _def, final Map<String, Integer> _headers,
                            final String[] _value,
                            final Integer _idx)
        throws EFapsException
    {
        Boolean ret;
        if (_def instanceof AttrSetDef) {
            final String barcodeColumn = ((AttrSetDef) _def).getProperty("BarcodeColumn");
            final String barcodeTypeIdColumn = ((AttrSetDef) _def).getProperty("BarcodeTypeIdColumn");
            final String barcodeTypeId = ((AttrSetDef) _def).getProperty("BarcodeTypeId");
            if (barcodeColumn != null && (barcodeTypeIdColumn != null || barcodeTypeId != null)) {
                LOG.debug("Row: {} - BarcodeColumn: {}, BarcodeTypeIdColumn: {}, BarcodeTypeId{} ", _idx, barcodeColumn,
                                barcodeTypeIdColumn, barcodeTypeId);
                ret = true;
            } else {
                LOG.error("Missing property. It requires one 'BarcodeColumn' "
                                + "and one of 'BarcodeTypeIdColumn' or 'BarcodeTypeId'");
                ret = false;
            }
        } else {
            LOG.error("Validation only works for AttrSetDef.");
            ret = false;
        }
        return ret;
    }

    @Override
    public void run(final Parameter _parameter, final AbstractDef _def, final Instance instance,
                    final Map<String, Integer> _headers,
                    final String[] _value, final Integer _idx)
        throws EFapsException
    {
        final String barcodeColumn = ((AttrSetDef) _def).getProperty("BarcodeColumn");
        final String barcodeTypeIdColumn = ((AttrSetDef) _def).getProperty("BarcodeTypeIdColumn");
        final String barcode = _value[_headers.get(barcodeColumn)].trim();
        final String barcodeTypeId;
        if (barcodeTypeIdColumn != null) {
            barcodeTypeId = _value[_headers.get(barcodeTypeIdColumn)].trim();
        } else {
            barcodeTypeId = ((AttrSetDef) _def).getProperty("BarcodeTypeId");
        }
        final AttributeSet set = AttributeSet.find(CIProducts.ProductAbstract.getType().getName(), "Barcodes");
        EQL.builder().insert(set)
                        .set(set.getAttributeName(), instance.getOid())
                        .set("Code", barcode)
                        .set("BarcodeType", barcodeTypeId)
                        .execute();
    }

}
