package ru.practicum.ewm.controller;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.service.handlers.RecommendationsHandler;
import ru.practicum.ewm.stats.protobuf.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.protobuf.RecommendedEventProto;
import ru.practicum.ewm.stats.protobuf.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.protobuf.UserPredictionsRequestProto;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class RecommendationsControllerGrpc extends ru.practicum.ewm.stats.protobuf.RecommendationsControllerGrpc.RecommendationsControllerImplBase {
    private final RecommendationsHandler recommendationsHandler;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.info("Поступил вызов метода getRecommendationsForUser с телом: {}", request);
            recommendationsHandler.getRecommendationsForUser(request).forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.info("Поступил вызов метода getSimilarEvents с телом: {}", request);
            recommendationsHandler.getSimilarEvents(request).forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }


    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.info("Поступил вызов метода getInteractionsCount с телом: {}", request);
            recommendationsHandler.getInteractionsCount(request).forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }
}