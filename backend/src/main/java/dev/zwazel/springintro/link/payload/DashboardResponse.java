package dev.zwazel.springintro.link.payload;

import java.util.List;

public record DashboardResponse(List<LinkResponse> links) {
}
