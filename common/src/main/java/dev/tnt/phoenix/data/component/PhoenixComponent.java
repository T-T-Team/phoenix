package dev.tnt.phoenix.data.component;

import dev.tnt.phoenix.api.GameInstanceAccess;

public abstract class PhoenixComponent {

    protected GameInstanceAccess instanceAccess;

    public void setInstanceAccess(GameInstanceAccess instanceAccess) {
        this.instanceAccess = instanceAccess;
    }
}
