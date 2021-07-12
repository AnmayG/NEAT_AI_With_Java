import com.almasb.fxgl.dsl.components.ExpireCleanComponent;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.entity.components.CollidableComponent;
import javafx.util.Duration;

import static com.almasb.fxgl.dsl.FXGL.entityBuilder;

//This is supposed to be undocumented right? I mean, you're never going to be calling it directly so it doesn't matter
public class GameFactory implements EntityFactory {
    @Spawns("Background")
    public Entity spawnBackground(SpawnData data){
        return entityBuilder(data)
                .type(Game.Type.BACKGROUND)
                .with(new BackgroundComponent())
                .build();
    }
    @Spawns("Player")
    public Entity spawnPlayer(SpawnData data){
        return entityBuilder(data)
                .type(Game.Type.PLAYER)
                .zIndex(1000)
                .viewWithBBox("Cannon.png")
                .with(new PlayerComponent(data.get("mode"), data.get("bads")))
                .build();
    }

    @Spawns("Bullet")
    public Entity spawnBullet(SpawnData data){
        ExpireCleanComponent expireCleanComponent = new ExpireCleanComponent(Duration.seconds(0.1));
        expireCleanComponent.pause();
        return entityBuilder(data)
                .type(Game.Type.BULLET)
                .zIndex(999)
                .viewWithBBox("Bullet.png")
                .with(expireCleanComponent)
                .with(new CollidableComponent(true))
                .with(new BulletComponent(data.get("attachment"), data.get("direction")))
                .build();
    }

    @Spawns("Enemy")
    public Entity spawnEnemy(SpawnData data){
        String s = "Target1.png";
        //if(Math.random()>0.5) s = "Target2.png";
        ExpireCleanComponent expireCleanComponent = new ExpireCleanComponent(Duration.ZERO);
        expireCleanComponent.pause();
        return entityBuilder(data)
                .type(Game.Type.ENEMY)
                .zIndex(data.getY()==0?997:998)
                .viewWithBBox(s)
                .with(expireCleanComponent)
                .with(new TargetComponent(data.get("speed")))
                .with(new CollidableComponent(true))
                .build();
    }
}
