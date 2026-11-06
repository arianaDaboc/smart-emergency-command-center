package com.smartemergency.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smartemergency.domain.*;
import com.smartemergency.repo.*;
import com.smartemergency.web.ApiException;
import com.smartemergency.web.Dto.*;

import java.util.List;

/** Manage Resources use case of the dispatcher. */
@Service
public class ResourceService {
    private final ResourceRepository resources;
    private final AssignmentRepository assignments;
    private final Mapper mapper;

    public ResourceService(ResourceRepository resources, AssignmentRepository assignments, Mapper mapper) {
        this.resources = resources;
        this.assignments = assignments;
        this.mapper = mapper;
    }

    public List<ResourceView> list() {
        return resources.findAllByOrderByCode().stream().map(mapper::resource).toList();
    }

    @Transactional
    public ResourceView create(ResourceRequest r) {
        String code = r.code().trim().toUpperCase();
        if (resources.existsByCode(code)) throw ApiException.conflict("The code " + code + " already exists");
        return mapper.resource(resources.save(new Resource(code, r.type(), r.lat(), r.lng())));
    }

    @Transactional
    public ResourceView setStatus(Long id, ResourceStatus status) {
        Resource r = resources.findById(id).orElseThrow(() -> ApiException.notFound("Resource"));
        if (status == ResourceStatus.BUSY) throw ApiException.badRequest("BUSY is set only by assigning the resource");
        if (r.getStatus() == ResourceStatus.BUSY) {
            Long inc = assignments.findFirstByResourceIdAndActiveTrue(id).map(a -> a.getIncident().getId()).orElse(null);
            throw ApiException.conflict("Resource is assigned to incident #" + inc);
        }
        r.setStatus(status);
        return mapper.resource(resources.save(r));
    }
}
