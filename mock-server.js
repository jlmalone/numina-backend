const express = require('express');
const cors = require('cors');

const app = express();
const PORT = 8080;

app.use(cors());
app.use(express.json());

// Mock class data
const mockClasses = [
  {
    id: '1',
    name: 'Morning Yoga Flow',
    description: 'Start your day with energizing yoga poses and breathing exercises',
    imageUrl: 'https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=800',
    classType: 'Yoga',
    intensity: 'low',
    startTime: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
    duration: 60,
    price: 25.00,
    currency: '$',
    location: {
      name: 'Zen Studio',
      address: '123 Main St, San Francisco, CA 94102',
      latitude: 37.7749,
      longitude: -122.4194
    },
    trainer: {
      id: 't1',
      name: 'Sarah Chen',
      bio: 'Certified yoga instructor with 10 years experience',
      photoUrl: 'https://i.pravatar.cc/150?img=1'
    },
    provider: {
      id: 'p1',
      name: 'Zen Studio',
      logoUrl: null
    },
    maxParticipants: 20,
    currentParticipants: 12
  },
  {
    id: '2',
    name: 'High Intensity Interval Training',
    description: 'Burn calories and build strength with this intense cardio workout',
    imageUrl: 'https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800',
    classType: 'HIIT',
    intensity: 'high',
    startTime: new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toISOString(),
    duration: 45,
    price: 30.00,
    currency: '$',
    location: {
      name: 'FitZone Gym',
      address: '456 Market St, San Francisco, CA 94103',
      latitude: 37.7849,
      longitude: -122.4094
    },
    trainer: {
      id: 't2',
      name: 'Marcus Johnson',
      bio: 'Former athlete and certified personal trainer',
      photoUrl: 'https://i.pravatar.cc/150?img=12'
    },
    provider: {
      id: 'p2',
      name: 'FitZone Gym',
      logoUrl: null
    },
    maxParticipants: 15,
    currentParticipants: 8
  },
  {
    id: '3',
    name: 'Spin & Cycle',
    description: 'High-energy cycling class with motivating music and coaching',
    imageUrl: 'https://images.unsplash.com/photo-1593079831268-3381b0db4a77?w=800',
    classType: 'Spin',
    intensity: 'medium',
    startTime: new Date(Date.now() + 3 * 24 * 60 * 60 * 1000).toISOString(),
    duration: 50,
    price: 28.00,
    currency: '$',
    location: {
      name: 'Cycle Studio',
      address: '789 Valencia St, San Francisco, CA 94110',
      latitude: 37.7649,
      longitude: -122.4214
    },
    trainer: {
      id: 't3',
      name: 'Emma Rodriguez',
      bio: 'Cycling enthusiast and motivational coach',
      photoUrl: 'https://i.pravatar.cc/150?img=5'
    },
    provider: {
      id: 'p3',
      name: 'Cycle Studio',
      logoUrl: null
    },
    maxParticipants: 25,
    currentParticipants: 18
  },
  {
    id: '4',
    name: 'CrossFit Fundamentals',
    description: 'Learn the basics of CrossFit with proper form and technique',
    imageUrl: 'https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800',
    classType: 'CrossFit',
    intensity: 'high',
    startTime: new Date(Date.now() + 4 * 24 * 60 * 60 * 1000).toISOString(),
    duration: 60,
    price: 35.00,
    currency: '$',
    location: {
      name: 'CrossFit Bay',
      address: '321 Folsom St, San Francisco, CA 94105',
      latitude: 37.7885,
      longitude: -122.3985
    },
    trainer: {
      id: 't4',
      name: 'Jake Thompson',
      bio: 'Level 2 CrossFit trainer with competition experience',
      photoUrl: 'https://i.pravatar.cc/150?img=15'
    },
    provider: {
      id: 'p4',
      name: 'CrossFit Bay',
      logoUrl: null
    },
    maxParticipants: 12,
    currentParticipants: 10
  },
  {
    id: '5',
    name: 'Pilates Core Strength',
    description: 'Build core strength and flexibility with mat Pilates',
    imageUrl: 'https://images.unsplash.com/photo-1518611012118-696072aa579a?w=800',
    classType: 'Pilates',
    intensity: 'low',
    startTime: new Date(Date.now() + 5 * 24 * 60 * 60 * 1000).toISOString(),
    duration: 55,
    price: 27.00,
    currency: '$',
    location: {
      name: 'Pilates Plus',
      address: '654 Mission St, San Francisco, CA 94105',
      latitude: 37.7875,
      longitude: -122.4005
    },
    trainer: {
      id: 't5',
      name: 'Lisa Martinez',
      bio: 'Certified Pilates instructor specializing in rehabilitation',
      photoUrl: 'https://i.pravatar.cc/150?img=9'
    },
    provider: {
      id: 'p5',
      name: 'Pilates Plus',
      logoUrl: null
    },
    maxParticipants: 18,
    currentParticipants: 14
  },
  {
    id: '6',
    name: 'Boxing Bootcamp',
    description: 'Learn boxing techniques while getting an amazing workout',
    imageUrl: 'https://images.unsplash.com/photo-1549719386-74dfcbf7dbed?w=800',
    classType: 'Boxing',
    intensity: 'high',
    startTime: new Date(Date.now() + 6 * 24 * 60 * 60 * 1000).toISOString(),
    duration: 60,
    price: 32.00,
    currency: '$',
    location: {
      name: 'Knockout Gym',
      address: '987 Howard St, San Francisco, CA 94103',
      latitude: 37.7865,
      longitude: -122.3995
    },
    trainer: {
      id: 't6',
      name: 'Tony Nguyen',
      bio: 'Former amateur boxer and certified fitness trainer',
      photoUrl: 'https://i.pravatar.cc/150?img=13'
    },
    provider: {
      id: 'p6',
      name: 'Knockout Gym',
      logoUrl: null
    },
    maxParticipants: 16,
    currentParticipants: 11
  }
];

// API endpoints
app.get('/api/v1/health', (req, res) => {
  res.json({ status: 'healthy' });
});

app.get('/api/v1/classes', (req, res) => {
  const { search, types, intensity, minPrice, maxPrice, page = '1', pageSize = '20' } = req.query;

  let filtered = [...mockClasses];

  if (search) {
    const query = search.toLowerCase();
    filtered = filtered.filter(c =>
      c.name.toLowerCase().includes(query) ||
      c.description.toLowerCase().includes(query)
    );
  }

  if (types) {
    const typeArray = types.split(',');
    filtered = filtered.filter(c => typeArray.includes(c.classType));
  }

  if (intensity) {
    const intensityArray = intensity.split(',');
    filtered = filtered.filter(c => intensityArray.includes(c.intensity));
  }

  if (minPrice) {
    filtered = filtered.filter(c => c.price >= parseFloat(minPrice));
  }

  if (maxPrice) {
    filtered = filtered.filter(c => c.price <= parseFloat(maxPrice));
  }

  // Return paginated response
  const pageNum = parseInt(page);
  const pageSizeNum = parseInt(pageSize);
  const startIndex = (pageNum - 1) * pageSizeNum;
  const endIndex = startIndex + pageSizeNum;
  const paginatedData = filtered.slice(startIndex, endIndex);

  res.json({
    data: paginatedData,
    total: filtered.length,
    page: pageNum,
    pageSize: pageSizeNum,
    totalPages: Math.ceil(filtered.length / pageSizeNum)
  });
});

app.get('/api/v1/classes/:id', (req, res) => {
  const classItem = mockClasses.find(c => c.id === req.params.id);
  if (classItem) {
    res.json(classItem);
  } else {
    res.status(404).json({ error: 'Class not found' });
  }
});

// Auth endpoints (mock)
app.post('/api/v1/auth/login', (req, res) => {
  res.json({
    token: 'mock-jwt-token',
    user: {
      id: 'user1',
      name: 'Test User',
      email: 'test@example.com',
      photoUrl: 'https://i.pravatar.cc/150?img=20',
      fitnessLevel: 7,
      fitnessInterests: ['Yoga', 'HIIT', 'Spin'],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }
  });
});

app.post('/api/v1/auth/register', (req, res) => {
  res.json({
    token: 'mock-jwt-token',
    user: {
      id: 'user1',
      name: req.body.name || 'New User',
      email: req.body.email,
      photoUrl: 'https://i.pravatar.cc/150?img=20',
      fitnessLevel: 5,
      fitnessInterests: [],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }
  });
});

app.get('/api/v1/users/me', (req, res) => {
  res.json({
    id: 'user1',
    name: 'Test User',
    email: 'test@example.com',
    photoUrl: 'https://i.pravatar.cc/150?img=20',
    bio: 'Fitness enthusiast',
    fitnessLevel: 7,
    fitnessInterests: ['Yoga', 'HIIT', 'Spin'],
    location: {
      address: 'San Francisco, CA',
      latitude: 37.7749,
      longitude: -122.4194
    },
    availability: [
      { day: 'Monday', timeSlots: ['morning', 'evening'] },
      { day: 'Wednesday', timeSlots: ['morning'] },
      { day: 'Friday', timeSlots: ['evening'] }
    ],
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  });
});

app.put('/api/v1/users/me', (req, res) => {
  res.json({
    id: 'user1',
    ...req.body,
    updatedAt: new Date().toISOString()
  });
});

app.listen(PORT, () => {
  console.log(`✅ Numina Mock API Server running on http://localhost:${PORT}`);
  console.log(`📊 Serving ${mockClasses.length} mock fitness classes`);
  console.log(`🔗 Classes endpoint: http://localhost:${PORT}/api/v1/classes`);
});
