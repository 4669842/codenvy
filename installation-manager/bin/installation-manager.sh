#!/bin/bash
#
#       /etc/rc.d/init.d/installat${PID_FILE}er
#
# Starts the startup operations
#
# chkconfig: 345 90 10
# description: Start script to launch installation manager

### BEGIN INIT INFO
# Provides: codenvy
# Default-Start:  2 3 4 5
# Default-Stop: 0 1 6
# Short-Description: start script
# Description: Start script to launch installation manager
### END INIT INFO

QUARTZ_OPTS="-Dorg.terracotta.quartz.skipUpdateCheck=true"
JVM_OPTS="$JVM_OPTS $QUARTZ_OPTS -Xmx256M"

USER=codenvy
APP_DIR=$HOME/installation-manager
APP=${APP_DIR}/installation-manager-binary.jar
PID_FILE=${APP_DIR}/codenvy.pid

# Starting tomcat
start () {
    echo "Starting ..."
    su - ${USER} -c "java $JVM_OPTS -jar $APP > $APP_DIR/log.txt & echo \$! >$PID_FILE"
}
 
# Stoping tomcat
stop () {
    if [ -f ${PID_FILE} ]
        then
            pid=`cat ${PID_FILE}`
    fi

    echo "Stopping ..."
    kill -TERM ${pid}

    if [ -n "$pid" ]
        then
            COUNTER=0
            STATUS="1"
            while [ ${STATUS} == "1" ]; do
                ps -fp ${pid}
                RETVAL=$?
                if [ ${RETVAL} -eq 0 ]
                    then
                        sleep 5
                        let COUNTER=COUNTER+1
                        if [ ${COUNTER} -eq 10 ]
                            then
                                kill -KILL ${pid}
                        fi
                    else
                        STATUS="0"
                        rm ${PID_FILE}
                fi
            done
    fi
}

status () {
    if [ -f ${PID_FILE} ]
        then
            pid=`cat ${PID_FILE}`
            ps -fp ${pid}
            RETVAL=$?
        else
            RETVAL=3
    fi

    if [ ${RETVAL} -eq 0 ]
        then
            echo "Installation Manager is running"
        else
            echo "Installation Manager is stopped"
    fi
    return ${RETVAL}
}

# See how we were called.
case "$1" in
  start)
    start
    status
    ;;
  stop)
    stop
    status
    ;;
  status)
    status
    ;;
  restart)
    stop
    start
    status
    ;;
  *)
    echo $"Usage: $0 {start|stop|status|restart}"
    exit 2
esac

exit $?
