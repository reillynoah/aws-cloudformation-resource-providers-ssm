package software.amazon.ssm.maintenancewindowtask;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.ssm.model.GetMaintenanceWindowTaskRequest;
import software.amazon.awssdk.services.ssm.model.GetMaintenanceWindowTaskResponse;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.ssm.maintenancewindowtask.translator.ExceptionTranslator;
import software.amazon.ssm.maintenancewindowtask.translator.request.GetMaintenanceWindowTaskTranslator;
import software.amazon.ssm.maintenancewindowtask.util.ClientBuilder;
import software.amazon.ssm.maintenancewindowtask.util.ResourceHandlerRequestToStringConverter;
import software.amazon.ssm.maintenancewindowtask.util.ResourceModelToStringConverter;

public class ReadHandler extends BaseHandler<CallbackContext> {

    private static final SsmClient SSM_CLIENT = ClientBuilder.getClient();

    private final GetMaintenanceWindowTaskTranslator getMaintenanceWindowTaskTranslator;
    private final ExceptionTranslator exceptionTranslator;
    private final ResourceHandlerRequestToStringConverter requestToStringConverter;

    ReadHandler() {
        this.getMaintenanceWindowTaskTranslator = new GetMaintenanceWindowTaskTranslator();
        this.exceptionTranslator = new ExceptionTranslator();
        this.requestToStringConverter = new ResourceHandlerRequestToStringConverter(new ResourceModelToStringConverter());
    }

    /**
     * Used for unit tests.
     *
     * @param getMaintenanceWindowTaskTranslator Translator between GetMaintenanceWindow and ResourceModel objects.
     * @param exceptionTranslator Translates service model exceptions.
     */
    ReadHandler(final GetMaintenanceWindowTaskTranslator getMaintenanceWindowTaskTranslator,
                final ExceptionTranslator exceptionTranslator,
                final ResourceHandlerRequestToStringConverter requestToStringConverter) {
        this.getMaintenanceWindowTaskTranslator = getMaintenanceWindowTaskTranslator;
        this.exceptionTranslator = exceptionTranslator;
        this.requestToStringConverter = requestToStringConverter;
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        logger.log(String.format("Processing ReadHandler request: %s", requestToStringConverter.convert(request)));

        final ResourceModel model = request.getDesiredResourceState();

        final ProgressEvent<ResourceModel, CallbackContext> progressEvent = new ProgressEvent<>();

        progressEvent.setStatus(OperationStatus.FAILED);

        if(StringUtils.isNullOrEmpty(model.getWindowId())||StringUtils.isNullOrEmpty(model.getWindowTaskId())){
            progressEvent.setErrorCode(HandlerErrorCode.InvalidRequest);
            progressEvent.setMessage("WindowId and WindowTaskId must be specified to get a maintenance window task.");
            return progressEvent;
        }

        final GetMaintenanceWindowTaskRequest getMaintenanceWindowTaskRequest =
                getMaintenanceWindowTaskTranslator.resourceModelToRequest(model);

        try {

            final GetMaintenanceWindowTaskResponse response =
                    proxy.injectCredentialsAndInvokeV2(getMaintenanceWindowTaskRequest, SSM_CLIENT::getMaintenanceWindowTask);

            final ResourceModel resourcemodel =
                    getMaintenanceWindowTaskTranslator.responseToResourceModel(response);

            progressEvent.setResourceModel(resourcemodel);
            progressEvent.setStatus(OperationStatus.SUCCESS);

        } catch (Exception e) {
            final BaseHandlerException cfnException = exceptionTranslator
                    .translateFromServiceException(e, getMaintenanceWindowTaskRequest, request.getDesiredResourceState());

            logger.log(cfnException.getCause().getMessage());

            throw cfnException;
        }

        return progressEvent;
    }
}
