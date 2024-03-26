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
package org.efaps.esjp.products.rest.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.esjp.common.rest.dto.AbstractDTO;

@EFapsUUID("745c8f9d-3882-45ee-8917-678999f69c63")
@EFapsApplication("eFapsApp-Products")
@JsonDeserialize(builder = ProductDTO.Builder.class)
public class ProductDTO
    extends AbstractDTO
{

    private final String name;
    private final String description;

    private ProductDTO(final Builder _builder)
    {
        super(_builder);
        this.name = _builder.name;
        this.description = _builder.description;
    }

    public String getName()
    {
        return this.name;
    }

    public String getDescription()
    {
        return this.description;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
        extends AbstractDTO.Builder<Builder, ProductDTO>
    {

        private String name;
        private String description;

        public Builder withName(final String _name)
        {
            this.name = _name;
            return this;
        }

        public Builder withDescription(final String _description)
        {
            this.description = _description;
            return this;
        }

        @Override
        public ProductDTO build()
        {
            return new ProductDTO(this);
        }
    }
}
