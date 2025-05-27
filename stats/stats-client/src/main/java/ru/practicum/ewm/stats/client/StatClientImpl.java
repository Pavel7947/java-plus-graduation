package ru.practicum.ewm.stats.client;

import com.google.protobuf.Timestamp;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.protobuf.*;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class StatClientImpl implements StatClient {
    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub client;
    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub userActionClient;

    @Override
    public void collectUserAction(long userId, long eventId, UserActionType actionType) {
        ActionTypeProto action = switch (actionType) {
            case LIKE -> ActionTypeProto.ACTION_LIKE;
            case REGISTER -> ActionTypeProto.ACTION_REGISTER;
            case VIEW -> ActionTypeProto.ACTION_VIEW;
        };
        UserActionProto request = UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(action)
                .setTimestamp(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000).build())
                .build();
        userActionClient.collectUserAction(request);
    }

    @Override
    public Stream<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                .setEventId(eventId)
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();
        Iterator<RecommendedEventProto> iterator = client.getSimilarEvents(request);
        return asStream(iterator);
    }

    @Override
    public Stream<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResult) {
        UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                .setUserId(userId)
                .setMaxResults(maxResult)
                .build();
        Iterator<RecommendedEventProto> iterator = client.getRecommendationsForUser(request);
        return asStream(iterator);
    }

    @Override
    public Stream<RecommendedEventProto> getInteractionsCount(List<Long> eventIdList) {
        InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addAllEventId(eventIdList)
                .build();
        Iterator<RecommendedEventProto> iterator = client.getInteractionsCount(request);
        return asStream(iterator);
    }

    private Stream<RecommendedEventProto> asStream(Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        );
    }
}
