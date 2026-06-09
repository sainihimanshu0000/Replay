import React from 'react';
import { View, Text, StyleSheet, Animated, Easing } from 'react-native';
import { useEffect, useRef } from 'react';

interface RecordingIndicatorProps {
  isRecording: boolean;
  isLoading?: boolean;
}

export const RecordingIndicator: React.FC<RecordingIndicatorProps> = ({
  isRecording,
  isLoading = false,
}) => {
  const pulseAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    if (isRecording && !isLoading) {
      const animation = Animated.loop(
        Animated.sequence([
          Animated.timing(pulseAnim, {
            toValue: 1,
            duration: 500,
            easing: Easing.inOut(Easing.ease),
            useNativeDriver: true,
          }),
          Animated.timing(pulseAnim, {
            toValue: 0,
            duration: 500,
            easing: Easing.inOut(Easing.ease),
            useNativeDriver: true,
          }),
        ])
      );

      animation.start();

      return () => animation.stop();
    }
  }, [isRecording, isLoading, pulseAnim]);

  const opacity = pulseAnim.interpolate({
    inputRange: [0, 1],
    outputRange: [0.4, 1],
  });

  if (!isRecording) {
    return null;
  }

  return (
    <View style={styles.container}>
      <Animated.View style={[styles.dot, { opacity }]} />
      <Text style={styles.text}>
        {isLoading ? 'Processing...' : 'Recording'}
      </Text>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
    paddingVertical: 8,
    backgroundColor: '#FF3B30',
    borderRadius: 20,
    alignSelf: 'flex-start',
  },
  dot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: '#fff',
    marginRight: 8,
  },
  text: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '600',
  },
});
