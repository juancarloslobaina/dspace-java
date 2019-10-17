/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.converter.BitstreamConverter;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.hateoas.BitstreamResource;
import org.dspace.app.rest.repository.CommunityRestRepository;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Community;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * This RestController takes care of the creation and deletion of Communities' nested objects
 * This class will typically receive the UUID of a Community and it'll perform logic on its nested objects
 */
@RestController
@RequestMapping("/api/" + CommunityRest.CATEGORY + "/" + CommunityRest.PLURAL_NAME)
public class CommunityRestController {

    /**
     * Regular expression in the request mapping to accept UUID as identifier
     */
    private static final String REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID =
        "{uuid:[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12}}";

    @Autowired
    protected Utils utils;

    @Autowired
    private BitstreamConverter bitstreamConverter;

    @Autowired
    private CommunityRestRepository communityRestRepository;

    @Autowired
    private CommunityService communityService;

    /**
     * This method will add a logo to the community.
     *
     * curl -X POST http://<dspace.restUrl>/api/core/communities/1c11f3f1-ba1f-4f36-908a-3f1ea9a557eb/logo' \
     *  -XPOST -H 'Content-Type: multipart/form-data' \
     *  -H 'Authorization: Bearer eyJhbGciOiJI...' \
     *  -F "file=@Downloads/test.png"
     *
     * Example:
     * <pre>
     * {@code
     * curl -X POST http://<dspace.restUrl>/api/core/communities/1c11f3f1-ba1f-4f36-908a-3f1ea9a557eb/logo' \
     *  -XPOST -H 'Content-Type: multipart/form-data' \
     *  -H 'Authorization: Bearer eyJhbGciOiJI...' \
     *  -F "file=@Downloads/test.png"
     * }
     * </pre>
     * @param request       The StandardMultipartHttpServletRequest that will contain the logo in its body
     * @param uuid          The UUID of the community
     * @return              The created bitstream
     * @throws SQLException If something goes wrong
     * @throws IOException  If something goes wrong
     * @throws AuthorizeException   If the user doesn't have the correct rights
     */
    @PreAuthorize("hasPermission(#uuid, 'COMMUNITY', 'WRITE')")
    @RequestMapping(method = RequestMethod.POST,
            value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID + "/logo",
            headers = "content-type=multipart/form-data")
    public ResponseEntity<ResourceSupport> createLogo(HttpServletRequest request, @PathVariable UUID uuid,
                                                      @RequestParam("file") MultipartFile uploadfile)
            throws SQLException, IOException, AuthorizeException {

        Context context = ContextUtil.obtainContext(request);

        Community community = communityService.find(context, uuid);
        if (community == null) {
            throw new ResourceNotFoundException(
                    "The given uuid did not resolve to a community on the server: " + uuid);
        }
        Bitstream bitstream = communityRestRepository.setLogo(context, community, uploadfile);

        BitstreamResource bitstreamResource = new BitstreamResource(bitstreamConverter.fromModel(bitstream), utils);
        context.complete();
        return ControllerUtils.toResponseEntity(HttpStatus.CREATED, null, bitstreamResource);
    }

    /**
     * This method is called when the user forgets to send a file
     * @param uuid          The UUID of the community
     */
    @PreAuthorize("hasPermission(#uuid, 'COMMUNITY', 'WRITE')")
    @RequestMapping(method = RequestMethod.POST,
            value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID + "/logo")
    public void createLogoInvalid(@PathVariable UUID uuid) {

        throw new UnprocessableEntityException("No file was given");
    }
}
