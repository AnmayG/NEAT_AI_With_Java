import com.almasb.fxgl.dsl.components.ExpireCleanComponent;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Point2D;

public class BulletComponent extends Component{
    private final Entity attachment;
    private final Point2D direction;

    /**
     * A bullet shot by a player
     * @param a The player that shot the bullet entity
     * @param direction The direction the bullet fires in
     */
    public BulletComponent(Entity a, Point2D direction){
        this.attachment = a;
        this.direction = direction;
    }

    public Entity getAttachment() {
        return attachment;
    }

    @Override
    public void onUpdate(double tpf) {
        super.onUpdate(tpf);
        entity.setRotation(Math.atan2(direction.getY(), direction.getX())*180/Math.PI);
        if(attachment.getComponent(PlayerComponent.class).getMode()==2) {
            entity.setOpacity(attachment.getComponent(PlayerComponent.class).getStudent().isChamp() ? 1 : 0);
        }
        entity.translate(direction.normalize().multiply(400*tpf));
        if(entity.getY() < 0){
            entity.getComponent(ExpireCleanComponent.class).resume();
        }
    }

    @Override
    public void onAdded() {
        if(entity.getY() < 0){
            entity.getComponent(ExpireCleanComponent.class).resume();
        }
    }
}
