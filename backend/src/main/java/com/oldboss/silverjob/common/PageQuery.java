package com.oldboss.silverjob.common;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PageQuery {
    private int page;
    private int pageSize;
    private int offset;
}
