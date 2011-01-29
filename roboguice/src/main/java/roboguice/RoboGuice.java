package roboguice;

import roboguice.config.AbstractRoboModule;
import roboguice.config.RoboModule;

import android.app.Application;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;

import java.util.ArrayList;
import java.util.WeakHashMap;

/**
 * BUG hashmap should also key off of stage
 */
public class RoboGuice {
    protected static WeakHashMap<Application,Injector> injectors = new WeakHashMap<Application,Injector>();
    protected static Stage DEFAULT_STAGE = Stage.PRODUCTION;

    private RoboGuice() {
    }

    public static Injector getInjector( Application context) {
        return getInjector(DEFAULT_STAGE, context);
    }

    public static Injector getInjector( Application application, Module... modules ) {
        return getInjector( DEFAULT_STAGE, application, modules );
    }

    // BUG doesn't allow overriding of RoboModule
    public static Injector getInjector( Stage stage, Application application, Module... modules ) {

        Injector rtrn = injectors.get(application);
        if( rtrn!=null )
            return rtrn;

        synchronized (RoboGuice.class) {
            rtrn = injectors.get(application);
            if( rtrn!=null )
                return rtrn;

            final ArrayList<Module> list = new ArrayList<Module>();
            final RoboModule roboModule = new RoboModule(application);

            list.add(roboModule);

            for ( Module m : modules ) {
                if( m instanceof AbstractRoboModule )
                    ((AbstractRoboModule)m).setRoboModule(roboModule);
                list.add(m);
            }

            rtrn = Guice.createInjector(stage, list);
            injectors.put(application,rtrn);

        }

        return rtrn;
    }



    public static Injector getInjector(Stage stage, Application application) {

        Injector rtrn = injectors.get(application);
        if( rtrn!=null )
            return rtrn;

        synchronized (RoboGuice.class) {
            rtrn = injectors.get(application);
            if( rtrn!=null )
                return rtrn;

            final int id = application.getResources().getIdentifier("roboguice_modules", "array", application.getPackageName());
            final String[] moduleNames = application.getResources().getStringArray(id);
            final ArrayList<Module> modules = new ArrayList<Module>();
            final RoboModule roboModule = new RoboModule(application);

            modules.add(roboModule);

            if (moduleNames != null) {
                try {
                    for (String name : moduleNames) {
                        final Module m = Class.forName(name).asSubclass(Module.class).getConstructor(Application.class).newInstance(application);
                        if( m instanceof AbstractRoboModule )
                            ((AbstractRoboModule)m).setRoboModule(roboModule);
                        modules.add( m );
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            rtrn = Guice.createInjector(stage, modules);
            injectors.put(application,rtrn);

        }

        return rtrn;
    }
}